package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
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

	private static final int SCROLL_SIZE = 1000;
	private static final int TIMEOUT = 600000;
	public final static String DEFAULT_INDEX = "genes";
	public final static String DEFAULT_TYPE = "gene";

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final Client client;
	private final String index;

	public ESGeneSearch(Client client) {
		this(client, DEFAULT_INDEX);
	}

	public ESGeneSearch(Client client, String index) {
		this.client = client;
		this.index = index;
	}

	@Override
	public List<Map<String, Object>> query(Collection<GeneQuery> queries,
			String... fieldNames) {
		final List<Map<String, Object>> results = new ArrayList<>();
		query(row -> {
			results.add(row);
		}, queries, fieldNames);
		return results;
	}

	@Override
	public void query(Consumer<Map<String, Object>> consumer,
			Collection<GeneQuery> queries, String... fieldNames) {

		log.info("Building query");
		QueryBuilder query = ESGeneSearchBuilder.buildQuery(queries
				.toArray(new GeneQuery[queries.size()]));

		log.info("Starting query");
		log.info(query.toString());

		boolean source = fieldNames.length == 0;
		SearchRequestBuilder request = client.prepareSearch(index)
				.setQuery(query).setFetchSource(source).addFields(fieldNames)
				.setSearchType(SearchType.SCAN)
				.setScroll(new TimeValue(TIMEOUT)).setSize(SCROLL_SIZE);
		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		// scroll until no hits are returned
		while (true) {
			processHits(consumer, source, response, fieldNames);
			response = client.prepareSearchScroll(response.getScrollId())
					.setScroll(new TimeValue(60000)).execute().actionGet();
			if (response.getHits().getHits().length == 0) {
				break;
			}
		}
		log.info("Retrieved all hits");

	}

	protected void processHits(Consumer<Map<String, Object>> consumer,
			boolean source, SearchResponse response, String... fieldNames) {

		for (SearchHit hit : response.getHits().getHits()) {
			Map<String, Object> row = new HashMap<>();
			row.put("_id", hit.getId());
			if (source) {
				row.put("source", hit.getSourceAsString());
			} else {
				for (String fieldName : fieldNames) {
					SearchHitField field = hit.field(fieldName);
					Object val = null;
					if (field != null) {
						val = field.getValues();
						if (((List<?>) val).size() == 1) {
							val = ((List<?>) val).get(0);
						}
					}
					row.put(fieldName, val);
				}
			}
			consumer.accept(row);
		}
	}

	@Override
	public QueryResult query(Collection<GeneQuery> queries,
			List<String> output, List<String> facets, int limit,
			List<QuerySort> sorts) {
		log.debug("Building query");
		QueryBuilder query = ESGeneSearchBuilder.buildQuery(queries
				.toArray(new GeneQuery[queries.size()]));

		log.info(query.toString());

		SearchRequestBuilder request = client
				.prepareSearch(index)
				.setQuery(query)
				.setFetchSource(output.toArray(new String[output.size()]), null)
				.setSize(limit);

		for (QuerySort sort : sorts) {
			log.info("Adding sort on " + sort.field);
			request.addSort(SortBuilders.fieldSort(sort.field)
					.order(SortOrder.valueOf(sort.direction.name()))
					.missing("_last"));
		}

		for (String facet : facets) {
			log.info("Adding facet on " + facet);
			AbstractAggregationBuilder builder = ESGeneSearchBuilder
					.buildAggregation(facet);
			if (builder != null)
				request.addAggregation(builder);
		}
		log.info("Starting query (limit " + limit + ")");
		log.debug("Query " + request.toString());

		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().getHits().length + "/"
				+ +response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		return new QueryResult(response.getHits().getTotalHits(),
				processResults(response), processAggregations(response));

	}

	protected List<Map<String, Object>> processResults(SearchResponse response) {
		return Arrays.stream(response.getHits().getHits())
				.map(hit -> processHit(hit)).collect(Collectors.toList());
	}

	protected Map<String, Object> processHit(SearchHit hit) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", hit.getId());
		System.out.println(hit.getSource());
		map.putAll(hit.getSource());
		hit.getFields();
		return map;
	}

	protected Map<String, Map<String, Long>> processAggregations(
			SearchResponse response) {
		Map<String, Map<String, Long>> facetResults = new HashMap<>();
		if (response.getAggregations() != null) {
			for (Entry<String, Aggregation> facet : response.getAggregations()
					.getAsMap().entrySet()) {
				log.debug("Getting facet on " + facet.getKey());
				Map<String, Long> facetResult = new HashMap<>();
				processAggregation(facetResult, facet.getValue());
				facetResults.put(facet.getKey(), facetResult);
			}
		}
		return facetResults;
	}

	protected void processAggregation(Map<String, Long> facetResults,
			Aggregation aggregation) {
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
}
