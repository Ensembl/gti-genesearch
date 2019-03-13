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

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.info.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static org.apache.commons.lang3.StringUtils.*;
import static org.ensembl.genesearch.Query.*;

/**
 * Class to translate from a list of nested {@link Query} objects to an
 * Elasticsearch {@link QueryBuilder} which can then be passed to Elastic.
 * 
 * This class supports all common and specialised {@link FieldType} queries,
 * including numeric ranges and genomic locations. Multiple values for a single
 * query are ORed together, and multiple queries are ANDed together. Support is
 * also provided for nested subqueries
 * 
 * This class also provides support for aggregration generation for basic facet
 * support.
 * 
 * @author dstaines
 *
 */
public class ESSearchBuilder {

    private static final Logger log = LoggerFactory.getLogger(ESSearchBuilder.class);

    static final String SEQ_REGION_FIELD = "seq_region_name";
    static final String GENOME_FIELD = "genome";
    static final String START_FIELD = "start";
    static final String END_FIELD = "end";
    static final String STRAND_FIELD = "strand";
    static final ScoreMode scoreMode = ScoreMode.Avg;

    private ESSearchBuilder() {
        //
    }

    /**
     * Generate an Elastic {@link QueryBuilder} for the supplied queries
     * 
     * @param type
     *            Elastic object type
     * @param qs
     * @return query builder for the supplied list of queries
     */
    public static QueryBuilder buildQuery(String type, Query... qs) {
        return buildQueryWithParents(type, new ArrayList<String>(), qs);
    }

    /**
     * Internal method for transforming a Query into. Supports nested subqueries
     * by iterative calling
     * 
     * @param type
     *            Elastic object type
     * @param parents
     *            optional list of parents for a nested subquery
     * @param qs
     * @return query builder for the supplied list of queries
     */
    protected static QueryBuilder buildQueryWithParents(String type, List<String> parents, Query... qs) {
        log.trace("Parents " + parents);
        if (qs.length == 1) {
            Query q = qs[0];
            QueryBuilder query;
            if (q.getType().equals(FieldType.NESTED)) {
                query = processNested(type, parents, q);
            } else {
                query = processSingle(type, parents, q);
            }
            return query;
        } else if (qs.length == 0) {
            log.trace("All IDs");
            return QueryBuilders.matchAllQuery();
        } else {
            log.trace("Multiples");
            return processMultiple(type, parents, qs);
        }
    }

    /**
     * Generate a boolean query by ANDing a list of queries together
     * 
     * @param type
     * @param parents
     * @param qs
     * @return query builder for the supplied list of queries
     */
    protected static BoolQueryBuilder processMultiple(String type, List<String> parents, Query... qs) {
        BoolQueryBuilder query = null;
        for (Query q : qs) {
            log.trace("Multiple " + q.getFieldName());
            QueryBuilder subQuery = buildQueryWithParents(type, parents, q);
            if (query == null) {
                query = QueryBuilders.boolQuery().must(subQuery);
            } else {
                query = query.must(subQuery);
            }
        }
        return query;
    }

    /**
     * Generate a single query clause that can then be combined with others.
     * Supports the full range of {@link FieldType}s plus negation.
     * 
     * @param type
     * @param parents
     * @param q
     * @return Elastic query
     */
    protected static QueryBuilder processSingle(String type, List<String> parents, Query q) {
        log.trace("Single " + q.getFieldName());
        String path = join(extendPath(parents, q), '.');
        QueryBuilder eq;
        switch (q.getType()) {
        case ID:
            if (parents.isEmpty())
                eq = processId(type, q);
            else
                eq = processTerm(type, q);
            break;
        case TEXT:
            eq = processText(path, q);
            break;
        case ONTOLOGY:
        case GENOME:
        case BOOLEAN:
        case TERM:
            eq = processTerm(path, q);
            break;
        case LOCATION:
            eq = processLocation(path, q);
            break;
        case NUMBER:
            eq = processNumber(path, q);
            break;
        default:
            throw new UnsupportedOperationException("Query type " + q.getType() + " not supported");
        }
        if (q.isNot()) {
            return QueryBuilders.boolQuery().mustNot(eq);
        } else {
            return eq;
        }
    }

    /**
     * Generate specialised query for IDs
     * 
     * @param type
     * @param q
     * @return elastic query
     */
    protected static QueryBuilder processId(String type, Query q) {
        return QueryBuilders.constantScoreQuery(QueryBuilders.idsQuery(type).addIds(q.getValues()));
    }

    /**
     * @param path
     * @param q
     * @return
     */
    protected static QueryBuilder processTerm(String path, Query q) {
        QueryBuilder query;
        if (q.getValues().length == 1) {
            query = QueryBuilders.termQuery(path, q.getValues()[0]);
        } else {
            query = QueryBuilders.termsQuery(path, q.getValues());
        }
        return QueryBuilders.constantScoreQuery(query);
    }

    /**
     * Generate text query with looser matching e.g. for descriptions
     * 
     * @param path
     * @param q
     * @return
     */
    protected static QueryBuilder processText(String path, Query q) {
        if (q.getValues().length == 1) {
            return QueryBuilders.matchQuery(path, q.getValues()[0]);
        } else {
            BoolQueryBuilder qb = QueryBuilders.boolQuery();
            for (String value : q.getValues()) {
                qb.should(QueryBuilders.matchQuery(path, value));
            }
            return qb;
        }
    }

    /**
     * Generate numeric query from one or more queries
     * 
     * @param path
     * @param q
     * @return
     */
    protected static QueryBuilder processNumber(String path, Query q) {
        if (q.getValues().length == 1) {
            return processNumber(path, q.getValues()[0]);
        } else {
            BoolQueryBuilder qb = QueryBuilders.boolQuery();
            for (String value : q.getValues()) {
                qb.should(processNumber(path, value));
            }
            return qb;
        }
    }

    /**
     * Generate a numeric query from a single value. Parses numeric strings to
     * support range-based queries e.g. 1-10, <1, >=10 etc.
     * 
     * @param path
     * @param value
     * @return
     */
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

    /**
     * Generate specialised query for one or more genomic locations
     * 
     * @param path
     * @param q
     * @return
     */
    protected static QueryBuilder processLocation(String path, Query q) {
        if (q.getValues().length == 1) {
            return processLocation(path, q.getValues()[0]);
        } else {
            BoolQueryBuilder qb = QueryBuilders.boolQuery();
            for (String value : q.getValues()) {
                qb.should(processLocation(path, value));
            }
            return qb;
        }
    }

    /**
     * Generate specialised query for a genomic location string of the form
     * name:start-end
     * 
     * @param locPath
     * @param q
     * @return
     */
    protected static QueryBuilder processLocation(String locPath, String q) {
        String path = locPath.replaceAll(".?location$", EMPTY);
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

    /**
     * Build a nested sub query against sub-documents
     * 
     * @param type
     * @param parents
     * @param q
     * @return
     */
    protected static QueryBuilder processNested(String type, List<String> parents, Query q) {
        QueryBuilder query;
        log.trace("Nested " + q.getFieldName());
        QueryBuilder subQuery = buildQueryWithParents(type, extendPath(parents, q), q.getSubQueries());
        query = QueryBuilders.nestedQuery(join(extendPath(parents, q), '.'), subQuery, scoreMode);
        return query;
    }

    /**
     * Utility to keep track of path to current sub-query as Elastic requires
     * this to be explicitly stated
     * 
     * @param parents
     * @param q
     * @return
     */
    protected static List<String> extendPath(List<String> parents, Query q) {
        List<String> newParents = new ArrayList<>(parents.size() + 1);
        newParents.addAll(parents);
        newParents.add(q.getFieldName());
        return newParents;
    }

    /**
     * Helper to generate an aggregration from a facet name size
     * 
     * @param facet
     * @param aggregationSize
     * @return
     */
    public static AbstractAggregationBuilder buildAggregation(String facet, int aggregationSize) {
        String[] subFacets = facet.split("\\.");
        AbstractAggregationBuilder builder = null;
        String path = EMPTY;

        for (int i = 0; i < subFacets.length; i++) {
            String subFacet = subFacets[i];
            path = prependPath(path, subFacet);
            if (i == subFacets.length - 1) {

                TermsAggregationBuilder subBuilder = AggregationBuilders.terms(subFacet).field(path).size(aggregationSize)
                        .order(BucketOrder.compound(BucketOrder.count(false), BucketOrder.key(true)));
                if (builder == null) {
                    builder = subBuilder;
                } else {
                    ((AggregationBuilder) builder).subAggregation(subBuilder);
                }
            } else {
                NestedAggregationBuilder subBuilder = AggregationBuilders.nested(subFacet.intern(), subFacet.toLowerCase()); //.path();
                if (builder == null) {
                    builder = subBuilder;
                } else {
                    ((AggregationBuilder) builder).subAggregation(subBuilder);
                }
            }
        }
        return builder;
    }

    /**
     * Prepend a path to the name, if set. Used by
     * {@link #buildAggregation(String, int)}
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
