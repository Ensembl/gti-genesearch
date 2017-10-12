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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.ensembl.genesearch.Query.GT;
import static org.ensembl.genesearch.Query.GTE;
import static org.ensembl.genesearch.Query.LOCATION;
import static org.ensembl.genesearch.Query.LT;
import static org.ensembl.genesearch.Query.LTE;
import static org.ensembl.genesearch.Query.RANGE;
import static org.ensembl.genesearch.Query.SINGLE_NUMBER;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.info.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to translate from a simplified nested key-value structure to an
 * Elasticsearch query
 * 
 * @author dstaines
 *
 */
public class ESSearchBuilder {

	private static final Logger log = LoggerFactory.getLogger(ESSearchBuilder.class);

	public static final String  SEQ_REGION_FIELD = "seq_region_name";
	public static final String START_FIELD = "start";
	public static final String END_FIELD = "end";
	public static final String STRAND_FIELD = "strand";

	private ESSearchBuilder() {
	}

	/**
	 * @param type
	 *            ES type
	 * @param geneQs
	 * @return query builder for the supplied list of queries
	 */
	public static QueryBuilder buildQuery(String type, Query... geneQs) {
		return buildQueryWithParents(type, new ArrayList<String>(), geneQs);
	}

	protected static QueryBuilder buildQueryWithParents(String type, List<String> parents, Query... geneQs) {
		log.trace("Parents " + parents);
		if (geneQs.length == 1) {
			Query geneQ = geneQs[0];
			QueryBuilder query;
			if (geneQ.getType().equals(FieldType.NESTED)) {
				query = processNested(type, parents, geneQ);
			} else {
				query = processSingle(type, parents, geneQ);
			}
			return query;
		} else if (geneQs.length == 0) {
			log.trace("All IDs");
			return QueryBuilders.matchAllQuery();
		} else {
			log.trace("Multiples");
			return processMultiple(type, parents, geneQs);
		}
	}

	protected static BoolQueryBuilder processMultiple(String type, List<String> parents, Query... geneQs) {
		BoolQueryBuilder query = null;
		for (Query geneQ : geneQs) {
			log.trace("Multiple " + geneQ.getFieldName());
			QueryBuilder subQuery = buildQueryWithParents(type, parents, geneQ);
			if (query == null) {
				query = QueryBuilders.boolQuery().must(subQuery);
			} else {
				query = query.must(subQuery);
			}
		}
		return query;
	}

	protected static QueryBuilder processSingle(String type, List<String> parents, Query geneQ) {
		log.trace("Single " + geneQ.getFieldName());
		String path = join(extendPath(parents, geneQ), '.');
		QueryBuilder q;
		switch (geneQ.getType()) {
		case ID:
			q = processId(type, geneQ);
			break;
		case TEXT:
			q =  processText(path, geneQ);
			break;
		case ONTOLOGY:
		case GENOME:
		case BOOLEAN:
		case TERM:
			q =  processTerm(path, geneQ);
			break;
		case LOCATION:
			q =  processLocation(path, geneQ);
			break;
		case NUMBER:
			q =  processNumber(path, geneQ);
			break;
		default:
			throw new UnsupportedOperationException("Query type " + geneQ.getType() + " not supported");
		}
		if(geneQ.isNot()) {
			return QueryBuilders.boolQuery().mustNot(q);
		} else {
			return q;
		}
	}

	protected static QueryBuilder processId(String type, Query geneQ) {
		return QueryBuilders.constantScoreQuery(QueryBuilders.idsQuery(type).addIds(geneQ.getValues()));
	}

	protected static QueryBuilder processTerm(String path, Query geneQ) {
		QueryBuilder query;
		if (geneQ.getValues().length == 1) {
			query = QueryBuilders.termQuery(path, geneQ.getValues()[0]);
		} else {
			query = QueryBuilders.termsQuery(path, geneQ.getValues());
		}
		return QueryBuilders.constantScoreQuery(query);
	}

	protected static QueryBuilder processText(String path, Query geneQ) {
		return QueryBuilders.matchQuery(path, StringUtils.join(geneQ.getValues(), ' '));
	}
	
	protected static QueryBuilder processNumber(String path, Query geneQ) {
		if (geneQ.getValues().length == 1) {
			return processNumber(path, geneQ.getValues()[0]);
		} else {
			BoolQueryBuilder qb = QueryBuilders.boolQuery();
			for (String value : geneQ.getValues()) {
				qb.should(processNumber(path, value));
			}
			return qb;
		}
	}

	protected static QueryBuilder processNumber(String path, String value) {
		Matcher m = SINGLE_NUMBER.matcher(value);
		if (m.matches()) {
			if (m.groupCount() == 1 || isEmpty(m.group(1))) {
				return QueryBuilders.constantScoreQuery(QueryBuilders.termQuery(path, value));
			} else {
				String op = m.group(1);
				RangeQueryBuilder rangeQ = QueryBuilders.rangeQuery(path);
				switch (op) {
				case GT:
					rangeQ.gt(m.group(2));
					break;
				case GTE:
					rangeQ.gte(m.group(2));
					break;
				case LT:
					rangeQ.lt(m.group(2));
					break;
				case LTE:
					rangeQ.lte(m.group(2));
					break;
				default:
					throw new UnsupportedOperationException("Unsupported numeric operator " + op);
				}
				return rangeQ;
			}
		} else {
			m = RANGE.matcher(value);
			if (m.matches()) {
				return QueryBuilders.rangeQuery(path).gte(m.group(1)).lte(m.group(2));
			} else {
				throw new UnsupportedOperationException("Cannot parse numeric query " + value);
			}
		}
	}

	protected static QueryBuilder processLocation(String path, Query geneQ) {
		if (geneQ.getValues().length == 1) {
			return processLocation(path, geneQ.getValues()[0]);
		} else {
			BoolQueryBuilder qb = QueryBuilders.boolQuery();
			for (String value : geneQ.getValues()) {
				qb.should(processLocation(path, value));
			}
			return qb;
		}
	}

	protected static QueryBuilder processLocation(String locPath, String q) {
		String path = locPath.replaceAll(".?location", EMPTY);
		Matcher m = LOCATION.matcher(q);
		if (!m.matches()) {
			throw new UnsupportedOperationException(q + " is not a valid location string");
		}
		/*
		 * note - we need to ensure start and end both lie in the range to deal
		 * with cross-origin genes where start>end
		 */
		BoolQueryBuilder qb = QueryBuilders.boolQuery()
				.must(QueryBuilders.termQuery(prependPath(path, SEQ_REGION_FIELD), m.group(1)))
				.must(QueryBuilders.rangeQuery(prependPath(path, START_FIELD)).from(m.group(2)).includeLower(true)
						.to(m.group(3)).includeUpper(true))
				.must(QueryBuilders.rangeQuery(prependPath(path, END_FIELD)).from(m.group(2)).includeLower(true)
						.to(m.group(3)).includeUpper(true));
		if (!isEmpty(m.group(5))) {
			qb.must(QueryBuilders.termQuery(prependPath(path, STRAND_FIELD), m.group(5)));
		}
		return qb;
	}

	protected static QueryBuilder processNested(String type, List<String> parents, Query geneQ) {
		QueryBuilder query;
		log.trace("Nested " + geneQ.getFieldName());
		QueryBuilder subQuery = buildQueryWithParents(type, extendPath(parents, geneQ), geneQ.getSubQueries());
		query = QueryBuilders.nestedQuery(join(extendPath(parents, geneQ), '.'), subQuery);
		return query;
	}

	protected static List<String> extendPath(List<String> parents, Query geneQ) {
		List<String> newParents = new ArrayList<>(parents.size() + 1);
		newParents.addAll(parents);
		newParents.add(geneQ.getFieldName());
		return newParents;
	}

	public static AbstractAggregationBuilder buildAggregation(String facet, int aggregationSize) {
		String[] subFacets = facet.split("\\.");
		AbstractAggregationBuilder builder = null;
		String path = EMPTY;
		for (int i = 0; i < subFacets.length; i++) {
			String subFacet = subFacets[i];
			path = prependPath(path, subFacet);
			if (i == subFacets.length - 1) {
				TermsBuilder subBuilder = AggregationBuilders.terms(subFacet).field(path).size(aggregationSize)
						.order(Terms.Order.compound(Terms.Order.count(false), Terms.Order.term(true)));
				if (builder == null) {
					builder = subBuilder;
				} else {
					((NestedBuilder) builder).subAggregation(subBuilder);
				}
			} else {
				NestedBuilder subBuilder = AggregationBuilders.nested(subFacet).path(path);
				if (builder == null) {
					builder = subBuilder;
				} else {
					((NestedBuilder) builder).subAggregation(subBuilder);
				}
			}
		}
		return builder;
	}

	/**
	 * Prepend a path to the name, if set
	 * 
	 * @param path
	 *            (can be null or blank)
	 * @param name
	 * @return name with optional path prepended
	 */
	protected static String prependPath(String path, String name) {
		if (isEmpty(path)) {
			return name;
		} else {
			return path + '.' + name;
		}
	}
}
