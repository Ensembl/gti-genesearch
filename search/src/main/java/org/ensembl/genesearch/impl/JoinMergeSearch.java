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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.FieldType;
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

    /**
     * default batch size
     */
    private static final int BATCH_SIZE = 1000;

    /**
     * special query term to trigger an inner join
     */
    private static final String INNER = "inner";

    /**
     * special field term to trigger a count
     */
    private static final String COUNT = "count";

    /**
     * Maximum number of "from" IDs to use in an inner join
     */
    private static final int JOIN_LIMIT = 1000;

    protected static enum MergeStrategy {
        MERGE, APPEND, APPEND_LIST;
    }

    protected static enum JoinType {
        TERM, RANGE;
    }

    /**
     * Class encapsulating information on how to join a source
     * 
     * @author dstaines
     *
     */
    public static class JoinStrategy {
        static JoinStrategy as(MergeStrategy merge, String fromKey, String toKey) {
            return new JoinStrategy(JoinType.TERM, merge, new String[] { fromKey }, new String[] { toKey }, null);
        }

        static JoinStrategy as(MergeStrategy merge, String fromKey, String toKey, String toGroupBy) {
            return new JoinStrategy(JoinType.TERM, merge, new String[] { fromKey }, new String[] { toKey }, toGroupBy);
        }

        static JoinStrategy asRange(MergeStrategy merge, String seqKey, String minKey, String maxKey,
                String locationKey) {
            return new JoinStrategy(JoinType.RANGE, merge, new String[] { seqKey, minKey, maxKey },
                    new String[] { locationKey }, null);
        }

        static JoinStrategy asGenomeRange(MergeStrategy merge, String genomeKey, String seqKey, String minKey,
                String maxKey, String toGenomeKey, String locationKey) {
            return new JoinStrategy(JoinType.RANGE, merge, new String[] { genomeKey, seqKey, minKey, maxKey },
                    new String[] { toGenomeKey, locationKey }, null);
        }

        protected JoinStrategy(JoinType type, MergeStrategy merge, String[] fromKey, String[] toKey, String toGroupBy) {
            this.type = type;
            this.merge = merge;
            this.fromKey = fromKey;
            this.toKey = toKey;
            this.toGroupBy = Optional.ofNullable(toGroupBy);
        }

        final JoinType type;
        final MergeStrategy merge;
        final String[] fromKey;
        final String[] toKey;
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

        public static final SubSearchParams build(Optional<SearchType> name, String[] keys, List<Query> queries,
                QueryOutput fields, JoinStrategy joinStrategy) {
            return new SubSearchParams(name, keys, queries, fields, joinStrategy);
        }

        final QueryOutput fields;
        final Optional<SearchType> name;
        final List<Query> queries;
        final String[] keys;
        final JoinStrategy joinStrategy;

        private SubSearchParams(Optional<SearchType> name, String[] keys, List<Query> queries, QueryOutput fields,
                JoinStrategy joinStrategy) {
            this.name = name;
            this.keys = keys;
            this.queries = queries;
            this.fields = fields;
            this.joinStrategy = joinStrategy;

            if (keys != null)
                for (String key : keys) {
                    if (!this.fields.getFields().contains(key)) {
                        this.fields.getFields().add(key);
                    }
                }
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }
    }

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
                if (query.getType().equals(FieldType.NESTED)
                        && query.getFieldName().equalsIgnoreCase(toName.get().name())) {
                    toQueries.addAll(Arrays.asList(query.getSubQueries()));
                } else {
                    fromQueries.add(query);
                }
            }

            Pair<QueryOutput, QueryOutput> decomposeOutputs = decomposeOutputs(output);

            return Pair.of(
                    SubSearchParams.build(fromName, joinStrategy.fromKey, fromQueries, decomposeOutputs.getLeft(),
                            joinStrategy),
                    SubSearchParams.build(toName, joinStrategy.toKey, toQueries, decomposeOutputs.getRight(),
                            joinStrategy));

        }

    }

    protected Pair<QueryOutput, QueryOutput> decomposeOutputs(QueryOutput output) {
        Optional<SearchType> fromName = Optional.of(getPrimarySearchType());
        Optional<SearchType> toName = getToName(output);

        // add the base fields
        QueryOutput fromOutput = new QueryOutput();
        QueryOutput toOutput = null;

        fromOutput.getFields().addAll(output.getFields());

        if (toName.isPresent()) {
            JoinStrategy joinStrategy = joinTargets.get(toName.get());
            // split subfields into "to" and "from"
            // NB: Could avoid adding into "from" here I guess
            for (Entry<String, QueryOutput> e : output.getSubFields().entrySet()) {
                if (toName.get().is(e.getKey())) {
                    toOutput = e.getValue();
                } else {
                    fromOutput.getSubFields().put(e.getKey(), e.getValue());
                }
            }

            if (joinStrategy.toGroupBy.isPresent()) {
                fromOutput.getFields().add(joinStrategy.toGroupBy.get());
            }
        }

        return Pair.of(fromOutput, toOutput);

    }

    private SearchType getPrimarySearchType() {
        return primarySearchType;
    }

    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
        // same as above, but batch it...
        // split up queries and fields
        Pair<SubSearchParams, SubSearchParams> qf = decomposeQueryFields(queries, fieldNames);

        SubSearchParams from = qf.getLeft();
        log.debug("'from' params:" + from);
        SubSearchParams to = qf.getRight();
        log.debug("'to' params:" + to);

        if (!to.name.isPresent()) {

            // we either have no target, or the target is a passthrough
            log.debug("Passing query through to primary search");
            provider.getSearch(getPrimarySearchType()).fetch(consumer, queries, fieldNames);

        } else if (to.joinStrategy.type == JoinType.RANGE) {

            log.debug("Executing join range query");
            fetchWithRangeJoin(consumer, from, to, isInner(to, from));

        } else if (to.joinStrategy.type == JoinType.TERM) {

            if (isInner(from, to)) {

                log.debug("Executing inner join term fetch");
                fetchWithTermJoin(consumer, innerTermJoinQuery(from, to), to);

            } else {

                log.debug("Executing join term fetch");
                fetchWithTermJoin(consumer, from, to);

            }
        } else {
            throw new UnsupportedOperationException("Unsupported join type " + to.joinStrategy.type);
        }
    }

    /**
     * Detect if join will be inner
     * 
     * @param from
     * @param to
     * @return
     */
    private boolean isInner(SubSearchParams from, SubSearchParams to) {
        boolean hasInner = false;
        Iterator<Query> iterator = to.queries.iterator();
        while (iterator.hasNext()) {
            Query next = iterator.next();
            if (INNER.equalsIgnoreCase(next.getFieldName())) {
                // remove the special inner term
                iterator.remove();
                hasInner = true;
                break;
            }
        }
        return hasInner;
    }

    /**
     * Use outer join mechanism to add optional "to" content to all rows in
     * "from" based on common terms in fields
     * 
     * @param consumer
     * @param from
     * @param to
     */
    protected void fetchWithTermJoin(Consumer<Map<String, Object>> consumer, SubSearchParams from, SubSearchParams to) {
        log.debug("Executing outer join query through to primary search");

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

    /**
     * Use outer join mechanism to add optional "to" content to all rows in
     * "from" based on positional overlap
     * 
     * @param consumer
     * @param from
     * @param to
     */
    protected void fetchWithRangeJoin(Consumer<Map<String, Object>> consumer, SubSearchParams from, SubSearchParams to,
            boolean inner) {
        // process in batches
        Search toSearch = provider.getSearch(to.name.get());
        provider.getSearch(from.name.get()).fetch(fromRow -> {
            // build a new query
            List<Query> toQueries = buildToRangeQuery(from, to, fromRow);
            // execute and merge "to" results onto existing "from"
            toSearch.fetch(toRow -> {
                addRangeJoinData(from, to, fromRow, toRow);
            }, toQueries, to.fields);
            if (!inner || fromRow.containsKey(to.name.get().toString())) {
                consumer.accept(fromRow);
            }
        }, from.queries, from.fields);
    }

    protected void addRangeJoinData(SubSearchParams from, SubSearchParams to, Map<String, Object> fromRow,
            Map<String, Object> toRow) {
        if (to.fields.getFields().contains(COUNT)) {
            incrementCount(fromRow, to.name.get().toString());
        } else {
            mergeResults(to, from, toRow).accept(fromRow);
        }
    }

    /**
     * Generate a new query set based on the positional information in the
     * result
     * 
     * @param from
     * @param to
     * @param result
     * @return
     */
    protected List<Query> buildToRangeQuery(SubSearchParams from, SubSearchParams to, Map<String, Object> result) {

        int n = 0;
        List<Query> qs = new ArrayList<>(to.queries.size() + 2);
        if (from.keys.length == 4) {
            // 0 is genome
            qs.add(new Query(FieldType.TERM, to.keys[n], String.valueOf(result.get(from.keys[n]))));
            n++;
        }
        // 1,2,3 is seq region, min, max
        qs.add(new Query(FieldType.LOCATION, to.keys[n],
                result.get(from.keys[n]) + ":" + result.get(from.keys[n + 1]) + "-" + result.get(from.keys[n + 2])));
        qs.addAll(to.queries);
        return qs;
    }

    protected void readFrom(Map<String, Object> r, SubSearchParams toParams, SubSearchParams fromParams,
            Map<String, List<Map<String, Object>>> resultsById, Map<String, Set<String>> ids) {

        Map<String, Map<String, Object>> objsForKey = DataUtils.getObjsForKey(r, fromParams.keys[0]);
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
                String key = toParams.joinStrategy.toGroupBy.get();
                String groupValue = e.getValue().get(key).toString();
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
            // terms only uses a single key
            String toKey = to.keys[0];
            if (to.joinStrategy.toGroupBy.isPresent()) {
                // for a join query, we need to use the group value as the term,
                // and the IDs as the values
                for (Entry<String, Set<String>> e : ids.entrySet()) {
                    Query[] qs = new Query[1 + to.queries.size()];
                    qs[0] = new Query(FieldType.TERM, toKey, false, e.getValue());
                    for (int i = 0; i < to.queries.size(); i++) {
                        qs[i + 1] = to.queries.get(i);
                    }
                    newQueries.add(new Query(FieldType.NESTED, e.getKey(), false, qs));
                }
            } else {
                newQueries.addAll(to.queries);
                newQueries.add(Query.expandQuery(toKey, false, ids.keySet()));
            }

            if (to.fields.getFields().size() == 2 && to.fields.getFields().contains(COUNT)) {
                provider.getSearch(to.name.get()).fetch(r -> {
                    for (String id : DataUtils.getObjValsForKey(r, toKey)) {
                        if (!StringUtils.isEmpty(id)) {
                            List<Map<String, Object>> results = resultsById.get(id);
                            if (results != null) {
                                results.stream().forEach(result -> incrementCount(result, to.name.get().toString()));
                            }
                        }
                    }
                }, newQueries, to.fields);
            } else {
                // run query on "to" and map values over
                provider.getSearch(to.name.get()).fetch(r -> {
                    for (String id : DataUtils.getObjValsForKey(r, toKey)) {
                        if (!StringUtils.isEmpty(id)) {
                            List<Map<String, Object>> results = resultsById.get(id);
                            if (results != null) {
                                results.stream().forEach(mergeResults(to, from, r));
                            }
                        }
                    }
                }, newQueries, to.fields);
            }
            ids.clear();
        }
    }

    /**
     * @param result
     * @param toName
     */
    @SuppressWarnings("unchecked")
    protected void incrementCount(Map<String, Object> result, String toName) {
        Object tgt = result.get(toName);
        if (tgt == null) {
            tgt = new HashMap<String, Object>();
            result.put(toName, tgt);
        }
        Object i = ((Map<String, Object>) tgt).get(COUNT);
        if (i == null) {
            ((Map<String, Object>) tgt).put(COUNT, 1);
        } else {
            ((Map<String, Object>) tgt).put(COUNT, (int) i + 1);
        }
    }

    protected Consumer<Map<String, Object>> mergeResults(SubSearchParams to, SubSearchParams from,
            Map<String, Object> r) {
        return fromR -> {
            // remove join field unless its the ID
            for (String key : from.keys) {
                if (!key.equals(this.getIdField())) {
                    fromR.remove(key);
                }
            }
            if (to.joinStrategy.merge == MergeStrategy.MERGE) {
                fromR.putAll(r);
            } else if (to.joinStrategy.merge == MergeStrategy.APPEND) {
                putOrAppend(to.name.get().toString(), r, fromR);
            } else if (to.joinStrategy.merge == MergeStrategy.APPEND_LIST) {
                appendToList(to.name.get().toString(), r, fromR);
            } else {
                throw new UnsupportedOperationException("Unsupported merge strategy " + to.joinStrategy.merge);
            }
        };
    }

    /**
     * Copy the value for the supplied key from the source hash to the target
     * hash, creating a list in the target if needed
     * 
     * @param key
     * @param src
     * @param tgt
     */
    protected void putOrAppend(String key, Map<String, Object> src, Map<String, Object> tgt) {
        Object existingResults = tgt.get(key);
        if (existingResults == null) {
            tgt.put(key, src);
        } else {
            appendToList(key, src, tgt);
        }
    }

    /**
     * Add the value to the specified list attribute, creating as required
     * 
     * @param key
     * @param src
     * @param tgt
     */
    protected void appendToList(String key, Map<String, Object> src, Map<String, Object> tgt) {
        Object existingResults = tgt.get(key);
        if (existingResults == null) {
            existingResults = new ArrayList<>();
            tgt.put(key, existingResults);
        }
        if (!List.class.isAssignableFrom(existingResults.getClass())) {
            List<Object> resultsList = new ArrayList<>();
            resultsList.add(existingResults);
            tgt.put(key, resultsList);
            existingResults = resultsList;
        }
        ((List) existingResults).add(src);
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
        log.debug("'from' params:" + from);
        SubSearchParams to = qf.getRight();
        log.debug("'to' params:" + to);

        if (!to.name.isPresent()) {

            // we either have no target, or the target is a passthrough
            log.debug("Passing query through to primary search");
            return provider.getSearch(getPrimarySearchType()).query(queries, output, facets, offset, limit, sorts);

        } else if (to.joinStrategy.type == JoinType.RANGE) {

            log.debug("Using range join to " + to.name);
            return queryWithRangeJoin(output, facets, offset, limit, sorts, from, to, isInner(from, to));

        } else if (to.joinStrategy.type == JoinType.TERM) {

            if (isInner(from, to)) {

                log.debug("Using inner term join to " + to.name);
                return queryWithTermJoin(output, facets, offset, limit, sorts, innerTermJoinQuery(from, to), to);

            } else {

                log.debug("Using term join to " + to.name);
                return queryWithTermJoin(output, facets, offset, limit, sorts, from, to);

            }

        } else {

            throw new UnsupportedOperationException("Unsupported join type " + to.joinStrategy.type);

        }

    }

    protected QueryResult queryWithRangeJoin(QueryOutput output, List<String> facets, int offset, int limit,
            List<String> sorts, SubSearchParams from, SubSearchParams to, boolean inner) {

        log.debug("Executing join query through primary using ranges " + (inner ? "" : "(inner)"));

        // query from first and generate a set of results
        log.debug("Executing 'from' query: " + from.queries);
        QueryResult fromResults = provider.getSearch(getPrimarySearchType()).query(from.queries, from.fields, facets,
                offset, limit, sorts);

        log.debug("Found " + fromResults.getResults().size() + " 'from' results");

        Search toSearch = provider.getSearch(to.name.get());
        log.debug("Applying to queries vs " + to.name.get());
        Iterator<Map<String, Object>> resultsI = fromResults.getResults().iterator();
        while (resultsI.hasNext()) {
            Map<String, Object> r = resultsI.next();
            List<Query> toQueries = buildToRangeQuery(from, to, r);
            log.debug("To queries: " + toQueries);
            // execute and merge "to" results onto existing "from"
            toSearch.fetch(t -> {
                addRangeJoinData(from, to, r, t);
            }, toQueries, to.fields);
            if (inner && !r.containsKey(to.name.get().toString())) {
                resultsI.remove();
            }
        }

        if (inner) {
            // result set size cannot be set accurately so set to -1
            fromResults.setResultCount(-1);
        }

        fromResults.getFields().clear();
        fromResults.getFields().addAll(this.getFieldInfo(output));

        return fromResults;

    }

    /**
     * Method for updating "from" for just those entries in "from" that are
     * joined in "to". This implementation assumes terms are used in the join
     * 
     * @param from
     * @param to
     * @return new "from" query
     */
    private SubSearchParams innerTermJoinQuery(SubSearchParams from, SubSearchParams to) {
        // example: search a: x:1, b:{c:2} where b is a separate dataset joined
        // by a.n to

        // b.m

        String fromKey = from.keys[0];
        String toKey = to.keys[0];

        // step 1: find all a.n where a.x=1 -> list[a.n]
        Set<String> fromIds = new HashSet<>(); // all "from" IDs matched in
                                               // "from"
        provider.getSearch(getPrimarySearchType()).fetch(r -> fromIds.addAll(DataUtils.getObjValsForKey(r, fromKey)),
                from.queries, QueryOutput.build(Arrays.asList(fromKey)));
        if (fromIds.size() > JOIN_LIMIT) {
            throw new UnsupportedOperationException(
                    "Inner joins not currently supported for more than " + JOIN_LIMIT + " entries");
        }
        log.debug("Retrieved " + fromIds.size() + " " + from.name + "." + fromKey);

        // step 2: query b for b.n=list[a.n] and b.c=2 and retrieve b.m ->
        // list[b.m]
        List<Query> qs = new ArrayList<>();
        qs.add(Query.expandQuery(new Query(FieldType.TERM, toKey, false, fromIds.toArray(new String[] {}))));
        qs.addAll(to.queries);
        Set<String> fromJoinedIds = new HashSet<>(); // all "from" IDs found
                                                     // in "to"
        provider.getSearch(to.name.get()).fetch(r -> fromJoinedIds.addAll(DataUtils.getObjValsForKey(r, toKey)), qs,
                QueryOutput.build(Arrays.asList(toKey)));
        log.debug("Retrieved " + fromJoinedIds.size() + " " + to.name + "." + toKey);

        // step 3: query is now n:list[b.m], b:{c.2} which can be passed
        // directly to query
        List<Query> newFromQ = new ArrayList<>(from.queries.size() + 1);
        newFromQ.add(
                Query.expandQuery(new Query(FieldType.TERM, fromKey, false, fromJoinedIds.toArray(new String[] {}))));
        // we still need the original x:1 query to avoid issues with n-m queries
        newFromQ.addAll(from.queries);
        return new SubSearchParams(from.name, from.keys, newFromQ, from.fields, from.joinStrategy);
    }

    /**
     * Run a query using "to" and "from" to join between two datasets
     * 
     * @param output
     * @param facets
     * @param offset
     * @param limit
     * @param sorts
     * @param from
     * @param to
     * @return result of query
     */
    protected QueryResult queryWithTermJoin(QueryOutput output, List<String> facets, int offset, int limit,
            List<String> sorts, SubSearchParams from, SubSearchParams to) {
        log.debug("Executing join query through primary");

        Search fromSearch = provider.getSearch(getPrimarySearchType());

        // query from first and generate a set of results
        QueryResult fromResults = fromSearch.query(from.queries, from.fields, facets, offset, limit, sorts);

        // hash results by ID and also create a new "to" search
        Map<String, List<Map<String, Object>>> resultsById = new HashMap<>();
        Map<String, Set<String>> ids = new HashMap<>();
        Search toSearch = provider.getSearch(to.name.get());
        fromResults.getResults().stream().forEach(r -> readFrom(r, to, from, resultsById, ids));
        // mop up leftovers
        mapTo(toSearch, to, from, resultsById, ids);
        fromResults.getFields().clear();
        fromResults.getFields().addAll(getFieldInfo(output));
        return fromResults;
    }

    @Override
    public QueryResult select(String name, int offset, int limit) {
        return provider.getSearch(getPrimarySearchType()).select(name, offset, limit);
    }

    @Override
    public List<FieldInfo> getFieldInfo(QueryOutput output) {

        // attempt to produce a complete set of fields for the output

        Optional<SearchType> toName = getToName(output);
        if (toName.isPresent()) {
            Pair<QueryOutput, QueryOutput> outputs = decomposeOutputs(output);
            JoinStrategy joinStrategy = joinTargets.get(toName.get());
            // from fields
            List<FieldInfo> info = Search.super.getFieldInfo(outputs.getLeft());
            if (joinStrategy.merge != MergeStrategy.MERGE) {
                // nested join field
                FieldInfo joinInfo = new FieldInfo(toName.get().getObjectName(), FieldType.NESTED);
                joinInfo.setSearch(false);
                joinInfo.setDisplay(true);
                joinInfo.setSort(false);
                joinInfo.setFacet(false);
                joinInfo.setDisplayName(toName.get().getObjectName());
                info.add(joinInfo);
            }
            // to fields
            Search toSearch = provider.getSearch(toName.get());
            for (FieldInfo f : toSearch.getFieldInfo(outputs.getRight())) {
                if (joinStrategy.merge == MergeStrategy.MERGE) {
                    // add joined fields as peers
                    info.add(f);
                } else {
                    // add joined fields as children
                    info.add(FieldInfo.clone(toName.get().getObjectName(), f));
                }
            }
            return info;
        } else {
            return Search.super.getFieldInfo(output);
        }
    }

    @Override
    public boolean up() {
        return provider.getSearch(getPrimarySearchType()).up();
    }

}