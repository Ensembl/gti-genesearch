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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneQuery.GeneQueryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESGeneSearchBuilder {

	private static final String ID_FIELD = "id";
	private static final Logger log = LoggerFactory
			.getLogger(ESGeneSearchBuilder.class);
	
	private ESGeneSearchBuilder() {
	}

	public static QueryBuilder buildQuery(GeneQuery... geneQs) {
		return buildQueryWithParents(new ArrayList<String>(), geneQs);
	}

	protected static QueryBuilder buildQueryWithParents(List<String> parents,
			GeneQuery... geneQs) {
		log.trace("Parents " + parents);
		if (geneQs.length == 1) {
			GeneQuery geneQ = geneQs[0];
			QueryBuilder query;
			if (geneQ.getType().equals(GeneQueryType.NESTED)) {
				query = processNested(parents, geneQ);
			} else {
				query = processSingle(parents, geneQ);
			}
			return query;
		} else if (geneQs.length == 0) {
			log.trace("All IDs");
			return QueryBuilders.matchAllQuery();
		} else {
			log.trace("Multiples");
			return processMultiple(parents, geneQs);
		}
	}

	protected static BoolQueryBuilder processMultiple(List<String> parents,
			GeneQuery... geneQs) {
		BoolQueryBuilder query = null;
		for (GeneQuery geneQ : geneQs) {
			log.trace("Multiple " + geneQ.getFieldName());
			QueryBuilder subQuery = buildQueryWithParents(parents, geneQ);
			if (query == null) {
				query = QueryBuilders.boolQuery().must(subQuery);
			} else {
				query = query.must(subQuery);
			}
		}
		return query;
	}

	protected static QueryBuilder processSingle(List<String> parents,
			GeneQuery geneQ) {
		QueryBuilder query;
		log.trace("Single " + geneQ.getFieldName());
		if (parents.size()==0 && ID_FIELD.equals(geneQ.getFieldName())) {
			query = QueryBuilders.idsQuery("gene").addIds(geneQ.getValues());
		} else {
			String path = StringUtils.join(extendPath(parents, geneQ), '.');
			if(geneQ.getType() == GeneQueryType.RANGE) {
				RangeQueryBuilder q = QueryBuilders.rangeQuery(path);
				if(geneQ.getStart()!=null) {
					q.from(geneQ.getStart());
				}
				if(geneQ.getEnd()!=null) {
					q.to(geneQ.getEnd());
				}
				query = q;
			} else if (geneQ.getValues().length == 1) {
				query = QueryBuilders.termQuery(path, geneQ.getValues()[0]);
			} else {
				query = QueryBuilders.termsQuery(path, geneQ.getValues());
			}
		}
		return QueryBuilders.constantScoreQuery(query);
	}

	protected static QueryBuilder processNested(List<String> parents,
			GeneQuery geneQ) {
		QueryBuilder query;
		log.trace("Nested " + geneQ.getFieldName());
		QueryBuilder subQuery = buildQueryWithParents(
				extendPath(parents, geneQ), geneQ.getSubQueries());
		query = QueryBuilders.nestedQuery(
				StringUtils.join(extendPath(parents, geneQ), '.'), subQuery);
		return query;
	}

	protected static List<String> extendPath(List<String> parents,
			GeneQuery geneQ) {
		List<String> newParents = new ArrayList<>(parents.size() + 1);
		newParents.addAll(parents);
		newParents.add(geneQ.getFieldName());
		return newParents;
	}

	public static AbstractAggregationBuilder buildAggregation(String facet) {
		String[] subFacets = facet.split("\\.");
		AbstractAggregationBuilder builder = null;
		String path = StringUtils.EMPTY;
		for (int i = 0; i < subFacets.length; i++) {
			String subFacet = subFacets[i];
			if (StringUtils.isEmpty(path)) {
				path = subFacet;
			} else {
				path = path + '.' + subFacet;
			}
			if (i == subFacets.length - 1) {
				TermsBuilder subBuilder = AggregationBuilders.terms(subFacet)
						.field(path);
				if (builder == null) {
					builder = subBuilder;
				} else {
					((NestedBuilder) builder).subAggregation(subBuilder);
				}
			} else {
				NestedBuilder subBuilder = AggregationBuilders.nested(subFacet)
						.path(path);
				if (builder == null) {
					builder = subBuilder;
				} else {
					((NestedBuilder) builder).subAggregation(subBuilder);
				}
			}
		}
		return builder;
	}
}
