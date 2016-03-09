package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.ensembl.genesearch.GeneSearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch.GeneQuery.GeneQueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESGeneSearchBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(ESGeneSearchBuilder.class);
	
	public static QueryBuilder buildQuery(GeneQuery... geneQs) {
		return buildQueryWithParents(new ArrayList<String>(), geneQs);
	}

	protected static QueryBuilder buildQueryWithParents(List<String> parents,
			GeneQuery... geneQs) {
		log.trace("Parents "+parents);
		if (geneQs.length == 1) {
			GeneQuery geneQ = geneQs[0];
			QueryBuilder query = null;
			if (geneQ.getType().equals(GeneQueryType.NESTED)) {
				log.trace("Nested "+geneQ.getFieldName());
				QueryBuilder subQuery = buildQueryWithParents(extendPath(parents, geneQ),
						geneQ.getSubQueries());
				query = QueryBuilders.nestedQuery(StringUtils.join(extendPath(parents, geneQ), '.'), subQuery);
			} else {
				log.trace("Single "+geneQ.getFieldName());
				if (geneQ.getFieldName().equals("_id")) {
					query = QueryBuilders.idsQuery("gene").addIds(
							geneQ.getValues());
				} else {
					String path = StringUtils.join(extendPath(parents, geneQ), '.');
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
		} else if(geneQs.length==0) {
			log.trace("All IDs");
			return QueryBuilders.matchAllQuery();
		} else {
			log.trace("Multiples");
			BoolQueryBuilder query = null;
			for (GeneQuery geneQ : geneQs) {
				log.trace("Multiple "+geneQ.getFieldName());
				QueryBuilder subQuery = buildQueryWithParents(parents,
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
