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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
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
	private final SearchRegistry provider;

	public JoinAwareSearch(SearchRegistry provider) {
		this.provider = provider;
	}

	protected abstract SearchType getDefaultType();

	protected abstract boolean isPassThrough(SearchType type);

	/**
	 * @param type
	 * @return
	 */
	protected abstract List<String> getFromJoinFields(SearchType type);

	/**
	 * @param type
	 * @return
	 */
	protected abstract String getToJoinField(SearchType type);

	/**
	 * Generate a query for a second search (e.g. variants) using a first search
	 * (e.g. genes). Can be overridden as required
	 * 
	 * @param joinType
	 *            target to join against
	 * @param queries
	 *            queries to use to build first set
	 * @return query to run against target
	 */
	protected List<Query> generateJoinQuery(SearchType joinType, List<Query> queries) {
		List<String> fields = getFromJoinFields(joinType);
		// by default take the first field
		String fieldName = fields.get(0);
		String target = null;
		if (fieldName.contains(".")) {
			target = fieldName.substring(0, fieldName.lastIndexOf('.'));
		}
		// create a list to hold the values
		List<String> vals = new ArrayList<>();

		provider.getSearch(getDefaultType()).fetch(doc -> {
			Object val = doc.get(fieldName);
			if (val != null) {
				if (Collection.class.isAssignableFrom(val.getClass())) {
					vals.addAll((Collection) val);
				} else {
					vals.add(val.toString());
				}
			}
		}, queries, fields, target);
		List<Query> qs = new ArrayList<>(1);
		qs.add(new Query(QueryType.TERM, getToJoinField(joinType), vals));
		return qs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, java.util.List, java.lang.String)
	 */
	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, List<String> fieldNames,
			String target) {
		fetch(consumer,queries,fieldNames,target,Collections.emptyList());
	}
	
	/**
	 * Retrieve all results matching the supplied queries, flattening to the specified target level
	 * 
	 * @param queries
	 * @param fieldNames
	 *            (if empty the whole document will be returned)
	 * @param target level to flatten to e.g. transcripts, transcripts.translations etc.
	 * @param targetQueries optional queries for join q
	 * @return
	 */
	public List<Map<String, Object>> fetch(List<Query> queries, List<String> fieldNames, String target, List<Query> targetQueries) {
		if (queries.isEmpty()) {
			throw new UnsupportedOperationException("Fetch requires at least one query term");
		}
		final List<Map<String, Object>> results = new ArrayList<>();
		fetch(row -> results.add(row), queries, fieldNames, target, targetQueries);
		return results;
	}
	
	/**
	 * Fetch with optional join target and query @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, java.util.List, java.lang.String) 
	 * @param consumer
	 * @param queries
	 * @param fieldNames
	 * @param target
	 * @param targetQueries optional queries for join query
	 */
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, List<String> fieldNames,
			String target, List<Query> targetQueries) {

		SearchType joinType = SearchType.findByName(target);

		if (isPassThrough(joinType)) {

			// passthrough
			provider.getSearch(getDefaultType()).fetch(consumer, queries, fieldNames, target);

		} else {

			// 1. generate a "to" query using the "from" query
			List<Query> joinQueries = generateJoinQuery(joinType, queries);
			joinQueries.addAll(targetQueries);
			// 2. pass the new query to the "to" search
			log.debug("Querying for " + joinType);
			provider.getSearch(joinType).fetch(consumer, joinQueries, fieldNames);

		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#query(java.util.List, java.util.List,
	 * java.util.List, int, int, java.util.List, java.lang.String)
	 */
	@Override
	public QueryResult query(List<Query> queries, List<String> output, List<String> facets, int offset, int limit,
			List<String> sorts, String target) {
		return query(queries, output, facets, offset, limit,
				sorts, target, Collections.emptyList());
	}
	
	/**
	 * Query with optional join target and query @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, java.util.List, java.lang.String) 
	 * @param consumer
	 * @param queries
	 * @param fieldNames
	 * @param target
	 * @param targetQueries optional queries for join query
	 */
	public QueryResult query(List<Query> queries, List<String> output, List<String> facets, int offset, int limit,
			List<String> sorts, String target, List<Query> targetQueries) {

		SearchType joinType = SearchType.findByName(target);
		if (isPassThrough(joinType)) {
			// passthrough
			return provider.getSearch(getDefaultType()).query(queries, output, facets, offset, limit, sorts, target);
		} else {
			// 1. generate a "to" query using the "from" query
			List<Query> joinQueries = generateJoinQuery(joinType, queries);
			joinQueries.addAll(targetQueries);
			// 2. pass the new query to the "to" search
			return provider.getSearch(joinType).query(joinQueries, output, facets, offset, limit, sorts, null);
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

}
