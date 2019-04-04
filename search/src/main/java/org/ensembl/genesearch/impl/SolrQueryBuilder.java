/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ensembl.genesearch.impl;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.ensembl.genesearch.Query.GT;
import static org.ensembl.genesearch.Query.GTE;
import static org.ensembl.genesearch.Query.LT;
import static org.ensembl.genesearch.Query.LTE;
import static org.ensembl.genesearch.Query.RANGE;
import static org.ensembl.genesearch.Query.SINGLE_NUMBER;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.ensembl.genesearch.Query;

/**
 * Utility to generate a Solr query from a list of {@link Query} objects
 * 
 * Supports NUMBER and TERM only. 
 * 
 * @author dstaines
 *
 */
public class SolrQueryBuilder {

    private static final String WILDCARD = "*";
    private static final String TO = " TO ";
    private static final String ALL_Q = "*:*";
    private static final String DELIMITER = ",";
    private static final String ASC = "asc";
    private static final String DESC = "desc";
    private static final String AND = " AND ";
    private static final String NOT = " NOT ";
    private static final String OR = " OR ";
    /**
     * Parameter name for Solr query
     */
    public static final String QUERY_PARAM = "q";
    /**
     * Parameter name for Solr sort
     */
    public static final String SORT_PARAM = "sort";
    /**
     * Parameter name for start (==offset)
     */
    public static final String START_PARAM = "start";
    /**
     * Parameter name for rows (==limit)
     */
    public static final String ROWS_PARAM = "rows";

    /**
     * @param queries
     *            list of query objects
     * @return solr query instance
     */
    public static SolrQuery build(List<Query> queries) {
        SolrQuery solrQ = new SolrQuery();
        StringBuilder qstr = new StringBuilder();
        if (queries.isEmpty()) {
            qstr.append(ALL_Q);
        } else {
            for (Query q : queries) {
                String clause;
                switch (q.getType()) {
                case TERM:
                    clause = termQuery(q);
                    break;
                case NUMBER:
                    clause = numberQuery(q);
                    break;
                case LOCATION:
                case NESTED:
                case TEXT:
                default:
                    throw new IllegalArgumentException("Solr querying does not support " + q.getType());
                }
                if (qstr.length() > 0) {
                    qstr.append(q.isNot() ? NOT : AND);
                }
                qstr.append(clause);
            }
        }
        solrQ.add(QUERY_PARAM, qstr.toString());
        return solrQ;
    }

    /**
     * @param q numeric query
     * @return solr query clause
     */
    protected static String numberQuery(Query q) {
        List<String> qs = Arrays.asList(q.getValues()).stream().map(SolrQueryBuilder::numberQuery)
                .collect(Collectors.toList());
        if (qs.size() > 1) {
            return q.getFieldName() + ':' + '(' + StringUtils.join(qs, OR) + ')';
        } else {
            return q.getFieldName() + ':' + qs.get(0);
        }
    }

    /**
     * @param value numeric query string
     * @return Solr formatted query string
     */
    protected static String numberQuery(String value) {
        Matcher m = SINGLE_NUMBER.matcher(value);
        String start = WILDCARD;
        String end = WILDCARD;
        boolean exclusive = false;
        if (m.matches()) {
            if (m.groupCount() == 1 || isEmpty(m.group(1))) {
                return value;
            } else {
                String op = m.group(1);
                switch (op) {
                case GT:
                    start = m.group(2);
                    exclusive = true;
                    break;
                case GTE:
                    start = m.group(2);
                    break;
                case LT:
                    end = m.group(2);
                    exclusive = true;
                    break;
                case LTE:
                    end = m.group(2);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported numeric operator " + op);
                }
            }
        } else {
            m = RANGE.matcher(value);
            if (m.matches()) {
                start = m.group(1);
                end = m.group(2);
            } else {
                throw new UnsupportedOperationException("Cannot parse numeric query " + value);
            }
        }
        if (exclusive) {
            return '{' + start + TO + end + '}';
        } else {
            return '[' + start + TO + end + ']';
        }
    }

    /**
     * @param q
     * @return solr query clause
     */
    protected static String termQuery(Query q) {
        if (q.getValues().length > 1) {
            return q.getFieldName() + ':' + '(' + StringUtils.join(q.getValues(), OR) + ')';
        } else {
            return q.getFieldName() + ':' + q.getValues()[0];
        }
    }

    /**
     * @param sort
     *            string e.g. start,-end,+name
     * @return Solr sort string e.g. start asc
     */
    public static String parseSort(String sort) {
        char s = sort.charAt(0);
        if (s == '+') {
            return sort.substring(1) + ' ' + ASC;
        } else if (s == '-') {
            return sort.substring(1) + ' ' + DESC;
        } else {
            return sort + ' ' + ASC;
        }
    }

    /**
     * @param sorts
     * @return Solr sort string
     */
    public static String parseSorts(Collection<String> sorts) {
        return sorts.stream().map(SolrQueryBuilder::parseSort).collect(Collectors.joining(DELIMITER));
    }

    private SolrQueryBuilder() {
    }

}
