/*
 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.output.ResultsRemodeller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of GeneSearch that uses Elasticsearch
 * 
 * @author dstaines
 *
 */
public class ESSearch implements Search {

	public static final String ALL_FIELDS = "*";
	public static final int DEFAULT_SCROLL_SIZE = 50000;
	public static final int DEFAULT_SCROLL_TIMEOUT = 60000;
	public static final String GENES_INDEX = "genes";
	public static final String GENE_TYPE = "gene";
	public static final String GENOME_TYPE = "genome";

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final Client client;
	private final String index;
	private final String type;

	public ESSearch(Client client, String index, String type) {
		this(client, index, type,
				Integer.parseInt(System.getProperty("es.scroll_size", String.valueOf(DEFAULT_SCROLL_SIZE))),
				Integer.parseInt(System.getProperty("es.scroll_timeout", String.valueOf(DEFAULT_SCROLL_TIMEOUT))));
	}

	private final int scrollSize;
	private final int scrollTimeout;

	public ESSearch(Client client, String index, String type, int scrollSize, int scrollTimeout) {
		this.client = client;
		this.index = index;
		this.type = type;
		this.scrollSize = scrollSize;
		this.scrollTimeout = scrollTimeout;
	}

	@Override
	public List<Map<String, Object>> fetch(List<Query> queries, List<String> fieldNames) {
		return fetch(queries, fieldNames, null);
	}

	@Override
	public List<Map<String, Object>> fetch(List<Query> queries, List<String> fieldNames, String target) {
		if (queries.isEmpty()) {
			throw new UnsupportedOperationException("Fetch requires at least one query term");
		}
		final List<Map<String, Object>> results = new ArrayList<>();
		fetch(row -> results.add(row), queries, fieldNames, target);
		return results;
	}

	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, List<String> fieldNames) {
		fetch(consumer, queries, fieldNames, null);
	}

	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, List<String> fieldNames,
			String target) {
		StopWatch watch = new StopWatch();

		int queryScrollSize = calculateScroll(fieldNames);

		log.debug("Using scroll size " + queryScrollSize);

		// if we have more terms than entries in our scroll, do it piecemeal
		if (queries.size() == 1) {
			Query query = queries.get(0);
			if (query.getType() == QueryType.TERM && query.getValues().length > queryScrollSize) {
				for (List<String> terms : ListUtils.partition(Arrays.asList(query.getValues()), queryScrollSize)) {
					log.info("Querying " + terms.size() + "/" + query.getValues().length);
					watch.start();
					fetch(consumer, Arrays.asList(new Query(query.getType(), query.getFieldName(), terms)), fieldNames,
							target);
					watch.stop();
					log.info("Queried " + terms.size() + "/" + query.getValues().length + " in " + watch.getTime()
							+ " ms");
					watch.reset();
				}
				return;
			}

		}

		log.info("Building fetch query");
		QueryBuilder query = ESSearchBuilder.buildQuery(type, queries.toArray(new Query[queries.size()]));

		log.debug(query.toString());

		SearchRequestBuilder request = client.prepareSearch(index).setQuery(query).setTypes(type);

		// force _doc order for more efficiency
		request.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC);

		if (fieldNames.contains(ALL_FIELDS) || fieldNames.isEmpty()) {
			fieldNames = Arrays.asList(ALL_FIELDS);
		}

		request.setFetchSource(fieldNames.toArray(new String[fieldNames.size()]), null);

		request.setScroll(new TimeValue(scrollTimeout)).setSize(queryScrollSize);

		log.info("Executing fetch request");
		log.debug(request.toString());
		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().totalHits() + " in " + response.getTookInMillis() + " ms");
		watch.start();
		processAllHits(consumer, response, target);
		watch.stop();
		log.info("Retrieved all hits in " + watch.getTime() + " ms");

	}

	private int calculateScroll(List<String> fieldNames) {
		// calculate a factor to adjust scroll by based on what we're retrieving
		// this is to try and balance speed and memory usage
		double scrollFactor = 0;
		for (String field : fieldNames) {
			if (ALL_FIELDS.equals(field)) {
				scrollFactor += 50;
			} else {
				// TODO more sophistication needed here at some point
				scrollFactor += 0.1;
			}
		}
		if (scrollFactor < 0.1) {
			scrollFactor = 0.1;
		} else if (scrollFactor > 50) {
			scrollFactor = 50;
		}
		return (int) (scrollSize / scrollFactor);
	}

	/**
	 * Process hits using scan/scroll
	 * 
	 * @param consumer
	 * @param response
	 * @return
	 */
	protected SearchResponse processAllHits(Consumer<Map<String, Object>> consumer, SearchResponse response,
			String target) {
		// scroll until no hits are returned
		int n = 0;
		StopWatch watch = new StopWatch();
		while (true) {
			log.debug("Processing scroll #" + (++n));
			processHits(consumer, response, target);
			log.debug("Preparing new scroll");
			watch.reset();
			watch.start();
			response = client.prepareSearchScroll(response.getScrollId())
					.setScroll(new TimeValue(DEFAULT_SCROLL_TIMEOUT)).execute().actionGet();
			watch.stop();
			log.debug("Prepared scroll #" + n + " in " + watch.getTime() + "ms");
			if (response.getHits().getHits().length == 0) {
				log.info("Scroll complete");
				break;
			}
		}
		return response;
	}

	/**
	 * Process hits using the specified consumer
	 * 
	 * @param consumer
	 * @param response
	 * @param target
	 */
	protected void processHits(Consumer<Map<String, Object>> consumer, SearchResponse response, String target) {
		SearchHit[] hits = response.getHits().getHits();
		StopWatch watch = new StopWatch();
		log.debug("Processing " + hits.length + " hits");
		watch.start();
		boolean flatten = !StringUtils.isEmpty(target);
		for (SearchHit hit : hits) {
			if (flatten) {
				for (Map<String, Object> o : ResultsRemodeller.flatten(processHit(hit), target)) {
					consumer.accept(o);
				}
			} else {
				consumer.accept(processHit(hit));
			}
		}
		watch.stop();
		log.debug("Completed processing " + hits.length + " hits in " + watch.getTime() + " ms");
	}

	@Override
	public QueryResult query(List<Query> queries, List<String> output, List<String> facets, int offset, int limit,
			List<String> sorts, String target) {
		log.debug("Building query");
		QueryBuilder query = ESSearchBuilder.buildQuery(type, queries.toArray(new Query[queries.size()]));

		log.info(query.toString());

		SearchRequestBuilder request = client.prepareSearch(index).setQuery(query)
				.setFetchSource(output.toArray(new String[output.size()]), null).setSize(limit).setFrom(offset)
				.setTypes(type);

		setFields(output, request);

		addSorts(sorts, request);

		addFacets(facets, request);

		log.info("Starting query (limit " + limit + ")");
		log.debug("Query " + request.toString());

		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().getHits().length + "/" + +response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		return new QueryResult(response.getHits().getTotalHits(), offset, limit, processResults(response, target),
				processAggregations(response));

	}

	private void setFields(List<String> output, SearchRequestBuilder request) {
		if (output.isEmpty()) {
			request.setFetchSource(false);
		} else {
			request.setFetchSource(output.toArray(new String[output.size()]), new String[0]);
		}
	}

	private void addFacets(List<String> facets, SearchRequestBuilder request) {
		for (String facet : facets) {
			log.info("Adding facet on " + facet);
			AbstractAggregationBuilder builder = ESSearchBuilder.buildAggregation(facet);
			if (builder != null)
				request.addAggregation(builder);
		}
	}

	private void addSorts(List<String> sorts, SearchRequestBuilder request) {
		for (String sortStr : sorts) {
			Sort sort = new Sort(sortStr);
			log.info("Adding " + sort.direction + " sort on '" + sort.name + "'");
			request.addSort(SortBuilders.fieldSort(sort.name).order(sort.direction).missing("_last"));
		}
	}

	protected List<Map<String, Object>> processResults(SearchResponse response, String target) {
		if (!StringUtils.isEmpty(target)) {
			return Arrays.stream(response.getHits().getHits())
					.map(hit -> ResultsRemodeller.flatten(processHit(hit), target)).flatMap(l -> l.stream())
					.collect(Collectors.toList());
		} else {
			return Arrays.stream(response.getHits().getHits()).map(hit -> processHit(hit)).collect(Collectors.toList());
		}
	}

	protected Map<String, Object> processHit(SearchHit hit) {
		Map<String, Object> map = new HashMap<>();
		map.put("id", hit.getId());
		if (hit.getSource() != null)
			map.putAll(hit.getSource());
		return map;
	}

	protected Map<String, Map<String, Long>> processAggregations(SearchResponse response) {
		Map<String, Map<String, Long>> facetResults = new HashMap<>();
		if (response.getAggregations() != null) {
			for (Entry<String, Aggregation> facet : response.getAggregations().getAsMap().entrySet()) {
				log.debug("Getting facet on " + facet.getKey());
				Map<String, Long> facetResult = new HashMap<>();
				processAggregation(facetResult, facet.getValue());
				facetResults.put(facet.getKey(), facetResult);
			}
		}
		return facetResults;
	}

	protected void processAggregation(Map<String, Long> facetResults, Aggregation aggregation) {
		if (Terms.class.isAssignableFrom(aggregation.getClass())) {
			log.info("Processing terms aggregation " + aggregation.getName());
			for (Bucket bucket : ((Terms) aggregation).getBuckets()) {
				log.debug(bucket.getKeyAsString() + ":" + bucket.getDocCount());
				facetResults.put(bucket.getKeyAsString(), bucket.getDocCount());
			}
		} else if (Nested.class.isAssignableFrom(aggregation.getClass())) {
			for (Aggregation subAgg : ((Nested) aggregation).getAggregations()) {
				log.debug("Processing sub-aggregation " + subAgg.getName());
				processAggregation(facetResults, subAgg);
			}
		} else {
			log.warn("Cannot handle " + aggregation.getClass());
		}
	}

	/**
	 * Helper to parse strings of the form +field, -field, field
	 * 
	 * @author dstaines
	 *
	 */
	private final class Sort {
		private final Pattern sortPattern = Pattern.compile("([+-])(.*)");
		public final String name;
		public final SortOrder direction;

		public Sort(String str) {
			// deal with URL encoding which treats + as a space
			str = str.replaceFirst("^ ", "+");
			Matcher m = sortPattern.matcher(str);
			if (m.matches()) {
				name = m.group(2);
				direction = "+".equals(m.group(1)) ? SortOrder.ASC : SortOrder.DESC;
			} else {
				name = str;
				direction = SortOrder.ASC;
			}
		}
	}

	@Override
	public List<Map<String, Object>> fetchByIds(String... ids) {
		return fetchByIds(Collections.emptyList(), ids);
	}
	
	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.Search#fetchByIds(java.util.List, java.lang.String[])
	 */
	@Override
	public List<Map<String, Object>> fetchByIds(List<String> fields, String... ids) {
		SearchRequestBuilder request = client.prepareSearch(index)
				.setQuery(new ConstantScoreQueryBuilder(QueryBuilders.idsQuery(type).addIds(ids)));
		if(!fields.isEmpty()){
			request.setFetchSource(fields.toArray(new String[]{}), new String[]{});
		}
		SearchResponse response = request.execute().actionGet();
		return processResults(response, null);
	}

	@Override
	public void fetchByIds(Consumer<Map<String, Object>> consumer, String... ids) {
		SearchRequestBuilder request = client.prepareSearch(index)
				.setQuery(new ConstantScoreQueryBuilder(QueryBuilders.idsQuery(type).addIds(ids)));
		SearchResponse response = request.execute().actionGet();
		processAllHits(consumer, response, null);
		log.info("Retrieved all hits");
	}
	
	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.Search#fetchById(java.util.List, java.lang.String)
	 */
	@Override
	public Map<String, Object> fetchById(List<String> fields, String id) {
		List<Map<String, Object>> genes = this.fetchByIds(fields, id);
		if (genes.isEmpty()) {
			return Collections.emptyMap();
		} else {
			return genes.get(0);
		}
	}

	@Override
	public Map<String, Object> fetchById(String id) {
		return fetchById(Collections.emptyList(), id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
	 */
	@Override
	public QueryResult select(String name, int offset, int limit) {

		QueryBuilder query;
		String[] fields;

		if (ESSearch.GENOME_TYPE.equals(type)) {
			query = QueryBuilders.functionScoreQuery(
					QueryBuilders.boolQuery()
							.should(QueryBuilders.matchPhrasePrefixQuery("organism.display_name", name).slop(10)
									.maxExpansions(limit).boost(4))
							.should(QueryBuilders.matchPhrasePrefixQuery("organism.scientific_name", name).slop(10)
									.maxExpansions(limit).boost(2))
							.should(QueryBuilders.matchPhrasePrefixQuery("organism.aliases", name).maxExpansions(limit)
									.slop(10))
							.should(QueryBuilders.matchPhrasePrefixQuery("organism.strain", name).maxExpansions(limit)
									.slop(10))
							.should(QueryBuilders.matchPhrasePrefixQuery("organism.serotype", name).maxExpansions(limit)
									.slop(10)),
					ScoreFunctionBuilders.fieldValueFactorFunction("is_reference").factor(2).modifier(Modifier.LOG1P));

			fields = new String[] { "id", "organism.display_name", "organism.scientific_name" };
		} else {
			throw new UnsupportedOperationException("select not implemented for " + type);
		}

		log.debug("Building query");

		log.info(query.toString());

		SearchRequestBuilder request = client.prepareSearch(index).setQuery(query)
				.setFetchSource(fields, new String[] {}).setSize(limit).setFrom(offset).setTypes(ESSearch.GENOME_TYPE);

		log.info("Starting query");
		log.debug("Query " + request.toString());

		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().getHits().length + "/" + +response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		return new QueryResult(response.getHits().getTotalHits(), offset, limit, processResults(response, null),
				processAggregations(response));

	}

}
