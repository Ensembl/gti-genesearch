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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.JsonDataTypeInfoProvider;
import org.ensembl.genesearch.output.ResultsRemodeller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;

/**
 * Implementation of GeneSearch that uses Elasticsearch
 * 
 * @author dstaines
 *
 */
public class ESSearch implements Search {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String ALL_FIELDS = "*";
	public static final String ID = "id";
	public static final int DEFAULT_SCROLL_SIZE = 50000;
	public static final int DEFAULT_SCROLL_TIMEOUT = 60000;
	private static final int DEFAULT_AGGREGATION_SIZE = 10;
	public static final String GENES_INDEX = "genes";
	public static final String GENE_ESTYPE = "gene";
	public static final String GENOME_ESTYPE = "genome";
	private static final String GENES = "genes";
	private static final String GENOMES = "genomes";

	private final Client client;
	private final String index;
	private final String type;
	private final List<DataTypeInfo> dataTypes;
	private final String defaultType;

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
		try {
			if (type.equals(GENE_ESTYPE)) {
				dataTypes = JsonDataTypeInfoProvider.load("/gene_datatype_info.json").getAll();
				defaultType = GENES;
			} else if (type.equals(GENOME_ESTYPE)) {
				dataTypes = JsonDataTypeInfoProvider.load("/genome_datatype_info.json").getAll();
				defaultType = GENOMES;
			} else {
				throw new IllegalArgumentException("Type " + type + " is not supported by ESSearch");
			}
		} catch (IOException e) {
			// cannot do anything about this but need to handle the exception
			throw new RuntimeException("Could not load the specified JSON resource");
		}
	}

	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput output) {

		String target = getTarget(output);

		List<String> fieldNames = output.getFields();

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
					fetch(consumer, Arrays.asList(new Query(query.getType(), query.getFieldName(), terms)), output);
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
		consumeAllHits(consumer, response, target);
		watch.stop();
		log.info("Retrieved all hits in " + watch.getTime() + " ms");

	}

	/**
	 * Determine the desired target from the output
	 * 
	 * @param output
	 * @return
	 */
	protected String getTarget(QueryOutput output) {
		Set<String> keySet = output.getSubFields().keySet();
		if (keySet.isEmpty()) {
			return null;
		} else if (keySet.size() <= 2) {
			return keySet.stream().filter(k -> !k.equals(defaultType)).findAny().orElse(null);
		} else {
			throw new IllegalArgumentException("Only single join accepted");
		}
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
	protected SearchResponse consumeAllHits(Consumer<Map<String, Object>> consumer, SearchResponse response,
			String target) {
		// scroll until no hits are returned
		int n = 0;
		StopWatch watch = new StopWatch();
		while (true) {
			log.debug("Processing scroll #" + (++n));
			consumeHits(consumer, response, target);
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
	protected void consumeHits(Consumer<Map<String, Object>> consumer, SearchResponse response, String target) {
		SearchHit[] hits = response.getHits().getHits();
		StopWatch watch = new StopWatch();
		log.debug("Processing " + hits.length + " hits");
		watch.start();
		boolean flatten = !StringUtils.isEmpty(target);
		for (SearchHit hit : hits) {
			if (flatten) {
				for (Map<String, Object> o : ResultsRemodeller.flatten(hitToMap(hit), target)) {
					consumer.accept(o);
				}
			} else {
				consumer.accept(hitToMap(hit));
			}
		}
		watch.stop();
		log.debug("Completed processing " + hits.length + " hits in " + watch.getTime() + " ms");
	}

	@Override
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {

		List<String> fieldNames = output.getFields();

		log.debug("Building query");
		// create an elastic querybuilder object from our queries
		QueryBuilder query = ESSearchBuilder.buildQuery(type, queries.toArray(new Query[queries.size()]));

		log.info(query.toString());

		// prepare a search request object using the query, fields, limits etc.
		SearchRequestBuilder request = client.prepareSearch(index).setQuery(query)
				.setFetchSource(fieldNames.toArray(new String[fieldNames.size()]), null).setSize(limit).setFrom(offset)
				.setTypes(type);

		setFields(fieldNames, request);

		addSorts(sorts, request);

		addFacets(facets, request, DEFAULT_AGGREGATION_SIZE);

		log.info("Starting query (limit " + limit + ")");
		log.debug("Query " + request.toString());

		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().getHits().length + "/" + +response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		return new QueryResult(response.getHits().getTotalHits(), offset, limit, getFieldInfo(output),
				processResults(response, getTarget(output)), processAggregations(response));

	}

	/**
	 * Set fields to retrieve. Note that if no fields are added, only the ID
	 * will be retrieved. '*' can be used to specify all fields.
	 * 
	 * @param output
	 * @param request
	 */
	private void setFields(List<String> output, SearchRequestBuilder request) {
		if (output.isEmpty()) {
			request.setFetchSource(false);
		} else {
			request.setFetchSource(output.toArray(new String[output.size()]), new String[0]);
		}
	}

	/**
	 * Set aggregations on a {@link SearchRequestBuilder}
	 * 
	 * @param facets
	 * @param request
	 */
	private void addFacets(List<String> facets, SearchRequestBuilder request, int aggregationSize) {
		for (String facet : facets) {
			log.info("Adding facet on " + facet);
			AbstractAggregationBuilder builder = ESSearchBuilder.buildAggregation(facet, aggregationSize);
			if (builder != null)
				request.addAggregation(builder);
		}
	}

	/**
	 * Adds sort directives to a {@link SearchRequestBuilder} given a list of
	 * field names
	 * 
	 * @param sorts
	 *            list of fields to sort on (optionally prefix with +/- to
	 *            indicate sort direction)
	 * @param request
	 *            request to add sorts to
	 */
	private void addSorts(List<String> sorts, SearchRequestBuilder request) {
		for (String sortStr : sorts) {
			Sort sort = new Sort(sortStr);
			log.info("Adding " + sort.direction + " sort on '" + sort.name + "'");
			request.addSort(SortBuilders.fieldSort(sort.name).order(sort.direction).missing("_last"));
		}
	}

	/**
	 * Transform all {@link SearchHit} in a {@link SearchResponse} into a
	 * {@link List} of {@link Map}s
	 * 
	 * @param response
	 * @param target
	 *            optional target for flattening e.g. transcripts
	 * @return collection representation of hit
	 */
	protected List<Map<String, Object>> processResults(SearchResponse response, String target) {
		if (!StringUtils.isEmpty(target)) {
			return Arrays.stream(response.getHits().getHits())
					.map(hit -> ResultsRemodeller.flatten(hitToMap(hit), target)).flatMap(l -> l.stream())
					.collect(Collectors.toList());
		} else {
			return Arrays.stream(response.getHits().getHits()).map(hit -> hitToMap(hit)).collect(Collectors.toList());
		}
	}

	/**
	 * Transform an instance of {@link SearchHit} into a generic {@link Map}
	 * 
	 * @param hit
	 *            ES hit to process
	 * @return map representation of hit
	 */
	protected Map<String, Object> hitToMap(SearchHit hit) {
		Map<String, Object> map = new HashMap<>();
		map.put("id", hit.getId());
		if (hit.getSource() != null)
			map.putAll(hit.getSource());
		return map;
	}

	/**
	 * Transform aggregation results from a {@link SearchResponse} into a
	 * generic collection
	 * 
	 * @param response
	 *            ES response from a search
	 * @return generic collection of objects.
	 */
	protected Map<String, Map<String, Long>> processAggregations(SearchResponse response) {
		Map<String, Map<String, Long>> facetResults = new HashMap<>();
		if (response.getAggregations() != null) {
			for (Aggregation facet : response.getAggregations().asList()) {
				log.debug("Getting facet on " + facet.getName());
				Map<String, Long> facetResult = new LinkedHashMap<>();
				processAggregation(facetResult, facet);
				facetResults.put(facet.getName(), facetResult);
			}
		}
		return facetResults;
	}

	/**
	 * Process a single {@link Aggregation} object into a Map of counts keyed by
	 * value
	 * 
	 * @param facetResults
	 * @param aggregation
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetchByIds(java.util.List,
	 * java.lang.String[])
	 */
	@Override
	public List<Map<String, Object>> fetchByIds(List<String> fields, String... ids) {
		SearchRequestBuilder request = client.prepareSearch(index)
				.setQuery(new ConstantScoreQueryBuilder(QueryBuilders.idsQuery(type).addIds(ids)));
		if (!fields.isEmpty()) {
			request.setFetchSource(fields.toArray(new String[] {}), new String[] {});
		}
		SearchResponse response = request.execute().actionGet();
		return processResults(response, null);
	}

	@Override
	public void fetchByIds(Consumer<Map<String, Object>> consumer, String... ids) {
		SearchRequestBuilder request = client.prepareSearch(index)
				.setQuery(new ConstantScoreQueryBuilder(QueryBuilders.idsQuery(type).addIds(ids)));
		SearchResponse response = request.execute().actionGet();
		consumeAllHits(consumer, response, null);
		log.info("Retrieved all hits");
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

		if (ESSearch.GENOME_ESTYPE.equals(type)) {
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
				.setFetchSource(fields, new String[] {}).setSize(limit).setFrom(offset).setTypes(ESSearch.GENOME_ESTYPE);

		log.info("Starting query");
		log.debug("Query " + request.toString());

		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().getHits().length + "/" + +response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		return new QueryResult(response.getHits().getTotalHits(), offset, limit,
				getFieldInfo(QueryOutput.build(Arrays.asList(fields))), processResults(response, null), processAggregations(response));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#getDataTypes()
	 */
	@Override
	public List<DataTypeInfo> getDataTypes() {
		return dataTypes;
	}

	@Override
	public List<FieldInfo> getFieldInfo(QueryOutput output) {
		// ES _always_ has ID
		List<String> fieldNames = output.getFields();
		if (!fieldNames.contains(ID)) {
			fieldNames = Lists.newLinkedList(fieldNames);
			fieldNames.add(0, ID);
		}
		return Search.super.getFieldInfo(QueryOutput.build(fieldNames));
	}

}
