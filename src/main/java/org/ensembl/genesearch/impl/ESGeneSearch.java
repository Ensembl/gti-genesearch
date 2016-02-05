package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESGeneSearch implements GeneSearch {

	private static final int SCROLL_SIZE = 1000;
	private static final int TIMEOUT = 600000;
	private final static String INDEX = "genes";

	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final Client client;

	public ESGeneSearch(Client client) {
		this.client = client;
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
		
		BoolQueryBuilder query = null;
		for (GeneQuery geneQ : queries) {
			QueryBuilder subQuery = null;
			if (geneQ.getFieldName().equals("_id")) {
				subQuery = QueryBuilders.idsQuery("gene").addIds(geneQ.getValues());
			} else {
				subQuery = QueryBuilders.termsQuery(geneQ.getFieldName(),
						geneQ.getValues());
			}
			if(query==null) {
				query = QueryBuilders.boolQuery().must(subQuery);				
			} else {
				query = query.must(subQuery);
			}
		}
		
		log.info("Starting query");

		boolean source = fieldNames.length == 0;
		SearchRequestBuilder request = client.prepareSearch(INDEX)
				.setPostFilter(query).setFetchSource(source)
				.addFields(fieldNames).setSearchType(SearchType.SCAN)
				.setScroll(new TimeValue(TIMEOUT)).setSize(SCROLL_SIZE);
		SearchResponse response = request
				.execute().actionGet();
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
						if(field==null) {
							throw new RuntimeException("Cannot find field "+fieldName);
						}
						Object val = field.getValue();
						if (val == null) {
							val = field.getValues();
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

}
