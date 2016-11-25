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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.FieldInfo.FieldType;
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
			return new JoinStrategy(merge, fromKey, toKey, null);
		}

		static JoinStrategy as(MergeStrategy merge, String fromKey, String toKey, String toGroupBy) {
			return new JoinStrategy(merge, fromKey, toKey, toGroupBy);
		}

		protected JoinStrategy(MergeStrategy merge, String fromKey, String toKey, String toGroupBy) {
			this.merge = merge;
			this.fromKey = fromKey;
			this.toKey = toKey;
			this.toGroupBy = Optional.ofNullable(toGroupBy);
		}

		final MergeStrategy merge;
		final String fromKey;
		final String toKey;
		/**
		 * Optional string to use as a grouping for fromKeys e.g. genome for
		 * sequence searches
		 */
		final Optional<String> toGroupBy;

	}

	/**
	 * Class encapsulating fields and queries used in a join query
	 * 
	 * @author dstaines
	 *
	 */
	public static class SubSearchParams {

		public static final SubSearchParams build(Optional<SearchType> name, String key, List<Query> queries,
				QueryOutput fields, JoinStrategy joinStrategy) {
			return new SubSearchParams(name, key, queries, fields, joinStrategy);
		}

		final QueryOutput fields;
		final Optional<SearchType> name;
		final List<Query> queries;
		final String key;
		final JoinStrategy joinStrategy;

		private SubSearchParams(Optional<SearchType> name, String key, List<Query> queries, QueryOutput fields,
				JoinStrategy joinStrategy) {
			this.name = name;
			this.key = key;
			this.queries = queries;
			this.fields = fields;
			this.joinStrategy = joinStrategy;
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

			JoinStrategy joinStrategy = joinTargets.get(toName.get());

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

			if (joinStrategy.toGroupBy.isPresent()) {
				fromOutput.getFields().add(joinStrategy.toGroupBy.get());
			}

			// find the to/from join fields and add to the output
			String fromField = joinStrategy.fromKey;
			String toField = joinStrategy.toKey;

			return Pair.of(SubSearchParams.build(fromName, fromField, fromQueries, fromOutput, joinStrategy),
					SubSearchParams.build(toName, toField, toQueries, toOutput, joinStrategy));

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
			Map<String, Set<String>> ids = new HashMap<>();
			provider.getSearch(from.name.get()).fetch(r -> {
				readFrom(r, to, from, resultsById, ids);
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

	protected void readFrom(Map<String, Object> r, SubSearchParams toParams, SubSearchParams fromParams,
			Map<String, List<Map<String, Object>>> resultsById, Map<String, Set<String>> ids) {

		Map<String, Map<String, Object>> objsForKey = DataUtils.getObjsForKey(r, fromParams.key);
		for (Entry<String, Map<String, Object>> e : objsForKey.entrySet()) {
			String fromId = e.getKey();
			if (StringUtils.isEmpty(fromId)) {
				continue;
			}
			List<Map<String, Object>> resultsForId = resultsById.get(fromId);
			if (resultsForId == null) {
				resultsForId = new ArrayList<>();
				resultsById.put(fromId, resultsForId);
			}
			resultsForId.add(e.getValue());
			if (toParams.joinStrategy.toGroupBy.isPresent()) {
				// where we're grouping IDs togther (e.g. sequences by genome)
				// we need to retrieve or create a set to add IDs to for that
				// genome
				String groupValue = e.getValue().get(toParams.joinStrategy.toGroupBy.get()).toString();
				Set<String> s = ids.get(groupValue);
				if (s == null) {
					s = new HashSet<>();
					ids.put(groupValue, s);
				}
				s.add(fromId);
			} else {
				// otherwise, we just reuse an empty set as the value
				ids.put(fromId, Collections.emptySet());
			}
		}

	}

	protected void mapTo(Search search, SubSearchParams to, SubSearchParams from,
			Map<String, List<Map<String, Object>>> resultsById, Map<String, Set<String>> ids) {
		if (!resultsById.isEmpty()) {
			// additional query joining to "to"
			List<Query> newQueries = new ArrayList<>();
			if (to.joinStrategy.toGroupBy.isPresent()) {
				// for a join query, we need to use the group value as the term,
				// and the IDs as the values
				for (Entry<String, Set<String>> e : ids.entrySet()) {
					Query[] qs = new Query[1 + to.queries.size()];
					qs[0] = new Query(QueryType.TERM, to.key, e.getValue());
					for (int i = 0; i < to.queries.size(); i++) {
						qs[i + 1] = to.queries.get(i);
					}
					newQueries.add(new Query(QueryType.NESTED, e.getKey(), qs));
				}
			} else {
				newQueries.addAll(to.queries);
				newQueries.add(Query.expandQuery(to.key, ids.keySet()));
			}

			// run query on "to" and map values over
			provider.getSearch(to.name.get()).fetch(r -> {
				String id = (String) r.get(to.key);
				List<Map<String, Object>> results = resultsById.get(id);
				if (results != null)
					results.stream().forEach(mergeResults(to, from, r));
			}, newQueries, to.fields);
			ids.clear();
		}
	}

	protected Consumer<Map<String, Object>> mergeResults(SubSearchParams to, SubSearchParams from,
			Map<String, Object> r) {
		return fromR -> {
			// remove join field unless its the ID
			if (!from.key.equals(this.getIdField())) {
				fromR.remove(from.key);
			}
			String key = to.name.get().name().toLowerCase();
			if (to.joinStrategy.merge == MergeStrategy.MERGE) {
				fromR.putAll(r);
			} else {
				fromR.put(key, r);
			}
		};
	}

	@Override
	public DataTypeInfo getDataType() {
		return provider.getSearch(getPrimarySearchType()).getDataType();
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

			log.debug("Executing join query through primary");

			// query from first and generate a set of results
			QueryResult fromResults = provider.getSearch(getPrimarySearchType()).query(from.queries, from.fields,
					facets, offset, limit, sorts);

			// hash results by ID and also create a new "to" search
			Map<String, List<Map<String, Object>>> resultsById = new HashMap<>();
			Map<String, Set<String>> ids = new HashMap<>();
			Search toSearch = provider.getSearch(to.name.get());
			fromResults.getResults().stream().forEach(r -> readFrom(r, to, from, resultsById, ids));
			// mop up leftovers
			mapTo(toSearch, to, from, resultsById, ids);
			fromResults.getFields().clear();
			fromResults.getFields().addAll(this.getFieldInfo(output));

			return fromResults;

		}

	}

	@Override
	public QueryResult select(String name, int offset, int limit) {
		return provider.getSearch(getPrimarySearchType()).select(name, offset, limit);
	}

	@Override
	public List<FieldInfo> getFieldInfo(QueryOutput fieldNames) {
		List<FieldInfo> fields = Search.super.getFieldInfo(fieldNames);
		for (String subField : fieldNames.getSubFields().keySet()) {
			Search search = provider.getSearch(SearchType.findByName(subField));
			if (search != null) {
				FieldInfo f = new FieldInfo();
				f.setName(search.getDataType().getName().toString());
				f.setFacet(false);
				f.setSort(false);
				f.setType(FieldType.OBJECT);
				if (!fields.contains(f)) {
					fields.add(f);
				}
			}
		}
		return fields;
	}

}