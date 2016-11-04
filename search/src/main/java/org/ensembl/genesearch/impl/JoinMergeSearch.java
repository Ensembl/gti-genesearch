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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for a search that can join between searches
 * 
 * @author dstaines
 *
 */
public abstract class JoinMergeSearch implements Search {

	protected static enum MergeStrategy {
		MERGE, APPEND;
	}

	/**
	 * Class encapsulating information on how to join a source
	 * 
	 * @author dstaines
	 *
	 */
	public static class JoinStrategy {
		static JoinStrategy as(MergeStrategy merge, String fromKey, String toKey) {
			return new JoinStrategy(merge, fromKey, toKey);
		}

		protected JoinStrategy(MergeStrategy merge, String fromKey, String toKey) {
			this.merge = merge;
			this.fromKey = fromKey;
			this.toKey = toKey;
		}

		final MergeStrategy merge;
		final String fromKey;
		final String toKey;
	}

	/**
	 * Class encapsulating fields and queries used in a join query
	 * 
	 * @author dstaines
	 *
	 */
	public static class SubSearchParams {

		public static final SubSearchParams build(Optional<SearchType> name, String key, List<Query> queries,
				QueryOutput fields, MergeStrategy mergeStrategy) {
			return new SubSearchParams(name, key, queries, fields, mergeStrategy);
		}

		final QueryOutput fields;
		final Optional<SearchType> name;
		final List<Query> queries;
		final String key;
		final MergeStrategy mergeStrategy;

		private SubSearchParams(Optional<SearchType> name, String key, List<Query> queries, QueryOutput fields,
				MergeStrategy mergeStrategy) {
			this.name = name;
			this.key = key;
			this.queries = queries;
			this.fields = fields;
			this.mergeStrategy = mergeStrategy;
			if (key != null && !this.fields.getFields().contains(key)) {
				this.fields.getFields().add(key);
			}
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}
	}

	/**
	 * default batch size
	 */
	private static final int BATCH_SIZE = 1000;
	protected final List<DataTypeInfo> dataTypes = new ArrayList<>();
	/**
	 * Search types for which we need a proper join
	 */
	protected final Map<SearchType, JoinStrategy> joinTargets = new HashMap<>();
	protected final Logger log = LoggerFactory.getLogger(this.getClass());
	/**
	 * primary search
	 */
	protected final SearchType primarySearchType;
	protected final SearchRegistry provider;

	public JoinMergeSearch(SearchType primarySearchType, SearchRegistry provider) {
		this.primarySearchType = primarySearchType;
		this.provider = provider;
		dataTypes.addAll(provider.getSearch(primarySearchType).getDataTypes());
	}
	
	protected int getBatchSize() {
		return BATCH_SIZE;
	}

	/**
	 * Split a set of queries and fields into "to" and "from" for a joined query
	 * 
	 * @param queries
	 * @param output
	 * @return pair of "from" and "to" {@link SubSearchParams}
	 */
	protected Pair<SubSearchParams, SubSearchParams> decomposeQueryFields(List<Query> queries, QueryOutput output) {

		Optional<SearchType> fromName = Optional.of(getPrimarySearchType());
		Optional<SearchType> toName = getToName(output);

		if (!toName.isPresent()) {

			// the basic fields that you don't need a join for
			return Pair.of(SubSearchParams.build(fromName, null, queries, output, null),
					SubSearchParams.build(toName, null, null, null, null));

		} else {

			List<Query> fromQueries = new ArrayList<>();
			List<Query> toQueries = new ArrayList<>();

			// split queries and output into from and to
			for (Query query : queries) {
				if (query.getType().equals(QueryType.NESTED)
						&& query.getFieldName().equalsIgnoreCase(toName.get().name())) {
					toQueries.addAll(Arrays.asList(query.getSubQueries()));
				} else {
					fromQueries.add(query);
				}
			}

			// add the base fields
			QueryOutput fromOutput = new QueryOutput();
			fromOutput.getFields().addAll(output.getFields());
			// split subfields into "to" and "from"
			QueryOutput toOutput = null;
			// NB: Could avoid adding into "from" here I guess
			for (Entry<String, QueryOutput> e : output.getSubFields().entrySet()) {
				if (e.getKey().equalsIgnoreCase(toName.get().name())) {
					toOutput = e.getValue();
				} else {
					fromOutput.getSubFields().put(e.getKey(), e.getValue());
				}
			}

			// find the to/from join fields and add to the output
			String fromField = joinTargets.get(toName.get()).fromKey;
			String toField = joinTargets.get(toName.get()).toKey;

			return Pair.of(
					SubSearchParams.build(fromName, fromField, fromQueries, fromOutput,
							joinTargets.get(toName.get()).merge),
					SubSearchParams.build(toName, toField, toQueries, toOutput, joinTargets.get(toName.get()).merge));
		}

	}

	public SearchType getPrimarySearchType() {
		return primarySearchType;
	}

	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
		// same as above, but batch it...
		// split up queries and fields
		Pair<SubSearchParams, SubSearchParams> qf = decomposeQueryFields(queries, fieldNames);

		SubSearchParams from = qf.getLeft();
		SubSearchParams to = qf.getRight();

		if (!to.name.isPresent()) {

			// we either have no target, or the target is a passthrough
			log.debug("Passing query through to primary search");
			provider.getSearch(getPrimarySearchType()).fetch(consumer, queries, fieldNames);

		} else {

			log.debug("Executing join query through to primary search with flattening");

			// process in batches
			Search toSearch = provider.getSearch(to.name.get());
			Map<String, List<Map<String, Object>>> resultsById = new HashMap<>();
			Set<String> ids = new HashSet<>();
			provider.getSearch(from.name.get()).fetch(r -> {
				readFrom(r, toSearch, to, from, resultsById, ids);
				if (resultsById.size() == getBatchSize()) {
					mapTo(toSearch, to, from, resultsById, ids);
					resultsById.values().stream().forEach(l -> l.stream().forEach(consumer));
					resultsById.clear();
				}
			}, from.queries, from.fields);
			mapTo(toSearch, to, from, resultsById, ids);
			resultsById.values().stream().forEach(l -> l.stream().forEach(consumer));

		}
	}

	protected void readFrom(Map<String, Object> r, Search search, SubSearchParams toParams, SubSearchParams fromParams,
			Map<String, List<Map<String, Object>>> resultsById, Set<String> ids) {

		Map<String, Map<String, Object>> objsForKey = DataUtils.getObjsForKey(r, fromParams.key);
		for (Entry<String, Map<String, Object>> e : objsForKey.entrySet()) {
			String fromId = e.getKey();
			List<Map<String, Object>> resultsForId = resultsById.get(fromId);
			if (resultsForId == null) {
				resultsForId = new ArrayList<>();
				resultsById.put(fromId, resultsForId);
			}
			resultsForId.add(e.getValue());
			ids.add(fromId);
		}

	}

	protected void mapTo(Search search, SubSearchParams to, SubSearchParams from,
			Map<String, List<Map<String, Object>>> resultsById, Set<String> ids) {
		if (!resultsById.isEmpty()) {
			// additional query joining to "to"
			to.queries.add(new Query(QueryType.TERM, to.key, ids));

			// run query on "to" and map values over
			provider.getSearch(to.name.get()).fetch(r -> {
				String id = (String) r.get(to.key);
				resultsById.get(id).stream().forEach(mergeResults(to, from, r));
			}, to.queries, to.fields);
			to.queries.remove(to.queries.size() - 1);
			ids.clear();
		}
	}

	protected Consumer<Map<String, Object>> mergeResults(SubSearchParams to, SubSearchParams from,
			Map<String, Object> r) {
		return fromR -> {
			fromR.remove(from.key);
			String key = to.name.get().name().toLowerCase();
			if (to.mergeStrategy == MergeStrategy.MERGE) {
				fromR.putAll(r);
			} else {
				fromR.put(key, r);
			}
		};
	}

	@Override
	public List<DataTypeInfo> getDataTypes() {
		return dataTypes;
	}

	public Optional<SearchType> getToName(QueryOutput output) {
		SearchType toName = null;
		// decomposition depends on the QueryOutput being one of the matched
		// do we have proper join targets?
		for (String field : output.getSubFields().keySet()) {
			SearchType t = SearchType.findByName(field);
			if (t != null && joinTargets.containsKey(t)) {
				toName = t;
				break;
			}
		}
		return Optional.ofNullable(toName);
	}

	@Override
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {

		// split up queries and fields
		Pair<SubSearchParams, SubSearchParams> qf = decomposeQueryFields(queries, output);

		SubSearchParams from = qf.getLeft();
		SubSearchParams to = qf.getRight();

		if (!to.name.isPresent()) {

			// we either have no target, or the target is a passthrough
			log.debug("Passing query through to primary search");
			return provider.getSearch(getPrimarySearchType()).query(queries, output, facets, offset, limit, sorts);

		} else {

			log.debug("Executing join query through to primary search with flattening");

			// query from first and generate a set of results
			QueryResult fromResults = provider.getSearch(getPrimarySearchType()).query(from.queries, from.fields,
					facets, offset, limit, sorts);

			// hash results by ID and also create a new "to" search
			Map<String, List<Map<String, Object>>> resultsById = new HashMap<>();
			Set<String> ids = new HashSet<>();
			Search toSearch = provider.getSearch(to.name.get());
			fromResults.getResults().stream().forEach(r -> readFrom(r, toSearch, to, from, resultsById, ids));
			// mop up leftovers
			mapTo(toSearch, to, from, resultsById, ids);

			return fromResults;

		}

	}

	@Override
	public QueryResult select(String name, int offset, int limit) {
		return provider.getSearch(getPrimarySearchType()).select(name, offset, limit);
	}

}