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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;

/**
 * Utility to generate a Solr query from a list of {@link Query} objects
 * 
 * @author dstaines
 *
 */
public class SolrQueryBuilder {

	private static final String ALL_Q = "*:*";
	private static final String DELIMITER = ",";
	private static final String ASC = "asc";
	private static final String DESC = "desc";
	private static final String AND = " AND ";
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
		List<String> clauses = new ArrayList<>(queries.size());
		if(queries.isEmpty()) {
			clauses.add(ALL_Q);
		} else {
		for (Query q : queries) {
			if (q.getType() != QueryType.TERM) {
				throw new IllegalArgumentException("Solr querying support limited to TERM only");
			}
			if (q.getValues().length > 1) {
				clauses.add(q.getFieldName() + ':' + '(' + StringUtils.join(q.getValues(), OR) + ')');
			} else {
				clauses.add(q.getFieldName() + ':' + q.getValues()[0]);
			}
		}
		}
		solrQ.add(QUERY_PARAM, StringUtils.join(clauses, AND));
		return solrQ;
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
