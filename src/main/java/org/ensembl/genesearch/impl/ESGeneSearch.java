package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.ensembl.genesearch.GeneSearch;
import org.ensembl.genesearch.GeneSearch.GeneQuery.GeneQueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESGeneSearch implements GeneSearch {

	private static final int SCROLL_SIZE = 1000;
	private static final int TIMEOUT = 600000;
	public final static String DEFAULT_INDEX = "genes";

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
		QueryBuilder query = buildQuery(queries.toArray(new GeneQuery[queries
				.size()]));

		log.info("Starting query");
		log.info(query.toString());

		boolean source = fieldNames.length == 0;
		SearchRequestBuilder request = client.prepareSearch(index)
				.setPostFilter(query).setFetchSource(source)
				.addFields(fieldNames).setSearchType(SearchType.SCAN)
				.setScroll(new TimeValue(TIMEOUT)).setSize(SCROLL_SIZE);
		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		// Scroll until no hits are returned
		while (true) {

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
			// next scroll response
			response = client.prepareSearchScroll(response.getScrollId())
					.setScroll(new TimeValue(60000)).execute().actionGet();
			// Break condition: No hits are returned
			if (response.getHits().getHits().length == 0) {
				break;
			}
		}
	}

	protected static QueryBuilder buildQuery(GeneQuery... geneQs) {
		return buildQuery(new ArrayList<String>(), geneQs);
	}

	protected static QueryBuilder buildQuery(List<String> parents,
			GeneQuery... geneQs) {
		if (geneQs.length == 1) {
			GeneQuery geneQ = geneQs[0];
			QueryBuilder query = null;
			if (geneQ.getType().equals(GeneQueryType.NESTED)) {
				parents = extendPath(parents, geneQ);
				QueryBuilder subQuery = buildQuery(parents,
						geneQ.getSubQueries());
				query = QueryBuilders.nestedQuery(StringUtils.join(parents, '.'), subQuery);
			} else {
				if (geneQ.getFieldName().equals("_id")) {
					query = QueryBuilders.idsQuery("gene").addIds(
							geneQ.getValues());
				} else {
					String path = StringUtils.join(parents, '.');
					if (geneQ.getValues().length == 1) {
						query = QueryBuilders.termQuery(path,
								geneQ.getValues()[0]);
					} else {
						query = QueryBuilders.termsQuery(path,
								geneQ.getValues());
					}
				}
			}
			return query;
		} else {
			BoolQueryBuilder query = null;
			for (GeneQuery geneQ : geneQs) {
				QueryBuilder subQuery = buildQuery(extendPath(parents, geneQ),
						geneQ);
				if (query == null) {
					query = QueryBuilders.boolQuery().must(subQuery);
				} else {
					query = query.must(subQuery);
				}
			}
			return query;
		}
	}

	protected static List<String> extendPath(List<String> parents,
			GeneQuery geneQ) {
		List<String> newParents = new ArrayList<>(parents.size() + 1);
		newParents.addAll(parents);
		newParents.add(geneQ.getFieldName());
		return newParents;
	}

}
