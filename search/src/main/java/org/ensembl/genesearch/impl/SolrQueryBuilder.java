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

	public static final String AND = " AND ";
	public static final String OR = " OR ";
	public static final String QUERY_PARAM = "q";

	/**
	 * @param queries
	 *            list of query objects
	 * @return solr query instance
	 */
	public static SolrQuery build(List<Query> queries) {
		SolrQuery solrQ = new SolrQuery();
		List<String> clauses = new ArrayList<>(queries.size());
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
		solrQ.add(QUERY_PARAM, StringUtils.join(clauses, AND));
		return solrQ;
	}

	private SolrQueryBuilder() {
	}

}
