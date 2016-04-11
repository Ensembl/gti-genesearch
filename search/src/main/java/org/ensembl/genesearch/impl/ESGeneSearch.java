package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.IdsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch;
import org.ensembl.genesearch.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of GeneSearch that uses Elasticsearch
 * 
 * @author dstaines
 *
 */
public class ESGeneSearch implements GeneSearch {

	public static final int DEFAULT_SCROLL_SIZE = 1000;
	public static final int DEFAULT_SCROLL_TIMEOUT = 60000;
	public static final String DEFAULT_INDEX = "genes";
	public static final String DEFAULT_TYPE = "gene";

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final Client client;
	private final String index;

	public ESGeneSearch(Client client) {
		this(client, DEFAULT_INDEX,
				Integer.parseInt(System.getProperty("es.scroll_size", String.valueOf(DEFAULT_SCROLL_SIZE))),
				Integer.parseInt(System.getProperty("es.scroll_timeout", String.valueOf(DEFAULT_SCROLL_TIMEOUT))));
	}

	private final int scrollSize;
	private final int scrollTimeout;

	public ESGeneSearch(Client client, String index, int scrollSize, int scrollTimeout) {
		this.client = client;
		this.index = index;
		this.scrollSize = scrollSize;
		this.scrollTimeout = scrollTimeout;
	}

	@Override
	public List<Map<String, Object>> fetch(Collection<GeneQuery> queries, List<String> fieldNames, List<String> sorts) {
		final List<Map<String, Object>> results = new ArrayList<>();
		fetch(row -> results.add(row), queries, fieldNames, sorts);
		return results;
	}

	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, Collection<GeneQuery> queries, List<String> fieldNames,
			List<String> sorts) {

		log.info("Building query");
		QueryBuilder query = ESGeneSearchBuilder.buildQuery(queries.toArray(new GeneQuery[queries.size()]));

		log.info("Starting query");
		log.info(query.toString());

		SearchRequestBuilder request = client.prepareSearch(index).setQuery(query).setScroll(new TimeValue(scrollSize))
				.setSize(scrollTimeout);

		if(sorts.isEmpty()) {
			sorts = Arrays.asList("_doc");
		}
		
		addSorts(sorts, request);

		request.setFetchSource(fieldNames.toArray(new String[fieldNames.size()]), null);

		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().totalHits() + " in " + response.getTookInMillis() + " ms");
		StopWatch watch = new StopWatch();
		watch.start();
		processAllHits(consumer, response);
		watch.stop();
		log.info("Retrieved all hits in " + watch.getTime() + " ms");

	}

	/**
	 * Process hits using scan/scroll
	 * 
	 * @param consumer
	 * @param response
	 * @return
	 */
	protected SearchResponse processAllHits(Consumer<Map<String, Object>> consumer, SearchResponse response) {
		// scroll until no hits are returned
		int n = 0;
		StopWatch watch = new StopWatch();
		while (true) {
			log.debug("Processing scroll #" + (++n));
			processHits(consumer, response);
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
	 */
	protected void processHits(Consumer<Map<String, Object>> consumer, SearchResponse response) {
		SearchHit[] hits = response.getHits().getHits();
		StopWatch watch = new StopWatch();
		log.debug("Processing " + hits.length + " hits");
		watch.start();
		for (SearchHit hit : hits) {
			consumer.accept(processHit(hit));
		}
		watch.stop();
		log.debug("Completed processing " + hits.length + " hits in " + watch.getTime() + " ms");
	}

	@Override
	public QueryResult query(Collection<GeneQuery> queries, List<String> output, List<String> facets, int limit,
			List<String> sorts) {
		log.debug("Building query");
		QueryBuilder query = ESGeneSearchBuilder.buildQuery(queries.toArray(new GeneQuery[queries.size()]));

		log.info(query.toString());

		SearchRequestBuilder request = client.prepareSearch(index).setQuery(query)
				.setFetchSource(output.toArray(new String[output.size()]), null).setSize(limit);

		setFields(output, request);

		addSorts(sorts, request);

		addFacets(facets, request);

		log.info("Starting query (limit " + limit + ")");
		log.debug("Query " + request.toString());

		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().getHits().length + "/" + +response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		return new QueryResult(response.getHits().getTotalHits(), processResults(response),
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
			AbstractAggregationBuilder builder = ESGeneSearchBuilder.buildAggregation(facet);
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

	protected List<Map<String, Object>> processResults(SearchResponse response) {
		return Arrays.stream(response.getHits().getHits()).map(hit -> processHit(hit)).collect(Collectors.toList());
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
		SearchRequestBuilder request = client.prepareSearch(index)
				.setQuery(new ConstantScoreQueryBuilder(new IdsQueryBuilder().addIds(ids)));
		SearchResponse response = request.execute().actionGet();
		return processResults(response);
	}

	@Override
	public void fetchByIds(Consumer<Map<String, Object>> consumer, String... ids) {
		SearchRequestBuilder request = client.prepareSearch(index)
				.setQuery(new ConstantScoreQueryBuilder(new IdsQueryBuilder().addIds(ids)));
		SearchResponse response = request.execute().actionGet();
		processAllHits(consumer, response);
		log.info("Retrieved all hits");
	}

	@Override
	public Map<String, Object> fetchById(String id) {
		List<Map<String, Object>> genes = this.fetchByIds(id);
		if (genes.isEmpty()) {
			return Collections.emptyMap();
		} else {
			return genes.get(0);
		}
	}

}
