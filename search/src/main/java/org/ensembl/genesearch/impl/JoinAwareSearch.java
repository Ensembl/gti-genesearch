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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of search that uses results from one query to run a second
 * query
 * 
 * @author dstaines
 *
 */
public abstract class JoinAwareSearch implements Search {
	
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	protected final SearchRegistry provider;

	public JoinAwareSearch(SearchRegistry provider) {
		this.provider = provider;
	}

	protected abstract SearchType getDefaultType();

	protected abstract boolean isPassThrough(SearchType type);

	protected abstract String getFromJoinField(SearchType type);

	protected abstract String getToJoinField(SearchType type);

	protected abstract int maxJoinSize(SearchType type);

	/**
	 * Generate a query for a second search (e.g. variants) using a first search
	 * (e.g. genes). Can be overridden as required
	 * 
	 * @param joinType
	 *            target to join against
	 * @param queries
	 *            queries to use to build first set
	 * @param fromOutput
	 * 			  output to merge from "from" database
	 * @return query to run against target
	 */
	protected Pair<List<Query>, Map<String,Map<String,Object>>> executeFromJoinQuery(SearchType joinType, List<Query> queries, QueryOutput fromOutput) {

		// divide queries into source and target
		List<Query> fromQueries = new ArrayList<>();
		List<Query> toQueries = new ArrayList<>();
		for(Query query: queries) {
			if(query.getFieldName().equalsIgnoreCase(getDefaultType().name())) {
				for(Query subQ: query.getSubQueries()) {
					toQueries.add(subQ);
				}
			} else {
				fromQueries.add(query);
			}
		}
		
		// create a list to hold the target values
		
		// find which join fields we need
		String fromField = getFromJoinField(joinType);
		
		// merge fromFields if needed
		if(!fromOutput.getFields().contains(fromField)) {
			fromOutput = new QueryOutput(fromOutput.getFields());
			fromOutput.getFields().add(0, fromField);
		}

		int maxSize = maxJoinSize(joinType);
		
		// execute the process
		Map<String,Map<String,Object>> fromResults = new HashMap<>();
		provider.getSearch(getDefaultType()).fetch(doc -> {
			Object val = doc.get(fromField);
			doc.remove(val);
			if (val != null) {
				if (Collection.class.isAssignableFrom(val.getClass())) {
					for(Object v: (Collection)val) {
						fromResults.put(v.toString(), doc);
					}
				} else {
					fromResults.put(val.toString(), doc);
				}
				if (fromResults.size() > maxSize) {
					throw new RuntimeException("Can only join a maximum of " + maxSize + " " + joinType.name());
				}
			}
		}, queries, fromOutput);

		toQueries.add(new Query(QueryType.TERM, getToJoinField(joinType), fromResults.keySet()));

		return Pair.of(toQueries, fromResults);
	}

	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer, java.util.List, java.util.List, java.lang.String, java.util.List)
	 */
	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {

		SearchType joinType =  getJoinType(fieldNames);

		if (isPassThrough(joinType)) {

			// passthrough
			provider.getSearch(getDefaultType()).fetch(consumer, queries, fieldNames);

		} else {

			// 1. generate a "to" query using the "from" query and a set of results for merge
			Pair<List<Query>, Map<String, Map<String, Object>>> jq = executeFromJoinQuery(joinType, queries, fieldNames);

			// 2. pass the new query to the "to" search
			log.debug("Querying for " + joinType);
			provider.getSearch(joinType).fetch(consumer, jq.getLeft(), fieldNames);

		}

	}

	/**
	 * Query with optional join target and query @see
	 * org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, java.util.List, java.lang.String)
	 * 
	 * @param consumer
	 * @param queries
	 * @param fieldNames
	 */
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {

		SearchType joinType = getJoinType(output);
		if (isPassThrough(joinType)) {
			// passthrough to the same search and let it figure out what to do
			return provider.getSearch(getDefaultType()).query(queries, output, facets, offset, limit, sorts);
		} else {
			// 1. generate a "to" query using the "from" query
			Pair<List<Query>, Map<String, Map<String, Object>>> jq = executeFromJoinQuery(joinType, queries, output);
			// 2. pass the new query to the "to" search
			return provider.getSearch(joinType).query(jq.getLeft(), output, facets, offset, limit, sorts);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetchByIds(java.util.List,
	 * java.lang.String[])
	 */
	@Override
	public List<Map<String, Object>> fetchByIds(List<String> fields, String... ids) {
		// pass through
		return provider.getSearch(getDefaultType()).fetchByIds(fields, ids);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ensembl.genesearch.Search#fetchByIds(java.util.function.Consumer,
	 * java.lang.String[])
	 */
	@Override
	public void fetchByIds(Consumer<Map<String, Object>> consumer, String... ids) {
		// pass through
		provider.getSearch(getDefaultType()).fetchByIds(consumer, ids);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
	 */
	@Override
	public QueryResult select(String name, int offset, int limit) {
		// pass through
		return provider.getSearch(getDefaultType()).select(name, offset, limit);
	}
	
	protected SearchType getJoinType(QueryOutput output) {
		SearchType defaultType = getDefaultType();
		for(String subField: output.getSubFields().keySet()) {
			SearchType t = SearchType.findByName(subField);
			if(!defaultType.equals(t)) {
				return t;
			}
		}
		return defaultType;
	}
	
}
