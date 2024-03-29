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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction.Modifier;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.NestedSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;

/**
 * Implementation of {@link Search} that uses Elasticsearch to store genes and
 * other features as nested documents.
 * <p>
 * {@link Query} and {@link QueryOutput} objects are translated into an Elastic
 * request using {@link ESSearchBuilder}. Large-scale retrieval via
 * {@link #fetch(List, QueryOutput)} is supported by using Elastic
 * scan-and-scroll for efficient retrieval without manual pagination.
 * <p>
 * Note that currently the native Elastic client is used. Elastic recommend that
 * from 6.0 onwards, the REST client is used. The interface should be the same,
 * but will require testing etc. and construction in a different way.
 *
 * @author dstaines
 */
public class ESSearch implements Search {

    protected final Logger log = LoggerFactory.getLogger(ESSearch.class);

    /*
     * Default fields
     */
    public static final String ALL_FIELDS = "*";
    public static final String ID = "id";

    /*
     * defaults for interacting with Elastic
     */
    public static final int DEFAULT_SCROLL_SIZE = 1000;
    public static final int DEFAULT_SCROLL_TIMEOUT = 6000;
    private static final int DEFAULT_AGGREGATION_SIZE = 10;

    /*
     * default values for searching different datatypes
     */
    public static final String GENES_INDEX = "genes";
    public static final String PROBES_INDEX = "probes";
    public static final String PROBESETS_INDEX = "probesets";
    public static final String GENOMES_INDEX = "genomes";
    public static final String VARIANTS_INDEX = "variants";
    public static final String MOTIFS_INDEX = "motifs";
    public static final String REGULATORY_FEATURES_INDEX = "regulatory_features";
    public static final String EXTERNAL_FEATURES_INDEX = "external_features";
    public static final String MIRNAS_INDEX = "mirnas";
    public static final String PEAKS_INDEX = "peaks";
    public static final String TRANSCRIPTION_FACTORS_INDEX = "transcription_factors";
    public static final String GENE_ESTYPE = "gene";
    public static final String GENOME_ESTYPE = "genome";
    public static final String VARIANT_ESTYPE = "variant";
    public static final String PROBE_ESTYPE = "probe";
    public static final String PROBESET_ESTYPE = "probeset";
    public static final String MOTIF_ESTYPE = "motif";
    public static final String REGULATORY_FEATURE_ESTYPE = "regulatory_feature";
    public static final String EXTERNAL_FEATURE_ESTYPE = "external_feature";
    public static final String MIRNA_ESTYPE = "mirna";
    public static final String PEAK_ESTYPE = "peak";
    public static final String TRANSCRIPTION_FACTOR_ESTYPE = "transcription_factor";
    private final Client client;
    private final String index;
    private final String type;
    private final DataTypeInfo dataType;

    /**
     * @param client   Elastic client
     * @param index    name of elastic index
     * @param type     name of object type within the index
     * @param dataType metadata about data type being searched
     */
    public ESSearch(Client client, String index, String type, DataTypeInfo dataType) {
        this(client, index, type, dataType,
                Integer.parseInt(System.getProperty("es.scroll_size", String.valueOf(DEFAULT_SCROLL_SIZE))),
                Integer.parseInt(System.getProperty("es.scroll_timeout", String.valueOf(DEFAULT_SCROLL_TIMEOUT))));
    }

    private final int scrollSize;
    private final int scrollTimeout;

    /**
     * @param client        Elastic client
     * @param index         name of elastic index
     * @param type          name of object type within the index
     * @param dataType      metadata about data type being searched
     * @param scrollSize    number of documents to retrieve during one scroll operation
     * @param scrollTimeout length of time to keep scroll open during retrieval
     */
    public ESSearch(Client client, String index, String type, DataTypeInfo dataType, int scrollSize,
                    int scrollTimeout) {
        this.client = client;
        this.index = index;
        this.type = type;
        this.scrollSize = scrollSize;
        this.scrollTimeout = scrollTimeout;
        this.dataType = dataType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
     * java.util.List, org.ensembl.genesearch.QueryOutput)
     */
    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput output) {

        List<String> fieldNames = output.getFields();

        StopWatch watch = new StopWatch();

        // determine
        int queryScrollSize = calculateScroll(fieldNames);

        log.debug("Using scroll size " + queryScrollSize);

        // if we have more terms than entries in our scroll, do it piecemeal
        if (queries.size() == 1) {
            Query query = queries.get(0);
            if (query.getType() == FieldType.TERM && query.getValues().length > queryScrollSize) {
                for (List<String> terms : ListUtils.partition(Arrays.asList(query.getValues()), queryScrollSize)) {
                    log.info("Querying " + terms.size() + "/" + query.getValues().length);
                    watch.start();
                    fetch(consumer,
                            Arrays.asList(new Query(query.getType(), query.getFieldName(), query.isNot(), terms)),
                            output);
                    watch.stop();
                    log.info("Queried " + terms.size() + "/" + query.getValues().length + " in " + watch.getTime()
                            + " ms");
                    watch.reset();
                }
                return;
            }

        }

        log.info("Building fetch query");
        QueryBuilder query = ESSearchBuilder.buildQuery(type, queries.toArray(new Query[queries.size()]));

        log.info(query.toString());

        SearchRequestBuilder request = client.prepareSearch(index).setQuery(query).setTypes(type);

        // force _doc order for more efficiency
        // FIXME check if still needed
        //request.addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC);

        if (fieldNames.contains(ALL_FIELDS) || fieldNames.isEmpty()) {
            fieldNames = Arrays.asList(ALL_FIELDS);
        }

        request.setFetchSource(fieldNames.toArray(new String[fieldNames.size()]), null);

        request.setScroll(new TimeValue(scrollTimeout)).setSize(queryScrollSize);

        log.info("Executing fetch request");
        log.debug(request.toString());
        SearchResponse response = request.execute().actionGet();
        log.info("Retrieved " + response.getHits().getTotalHits() + " in " + response.getTook().getMillis() + " ms");
        watch.start();
        consumeAllHits(consumer, response);
        watch.stop();
        log.info("Retrieved all hits in " + watch.getTime() + " ms");

    }

    /**
     * calculate a scroll size based on what we're retrieving this is to try and
     * balance speed and memory usage. The more fields we retrieve, the smaller
     * the number of documents per scroll
     *
     * @param fieldNames
     * @return scroll size
     */
    private int calculateScroll(List<String> fieldNames) {

        // calculate a factor to adjust scroll by
        double scrollFactor = 0;
        for (String field : fieldNames) {
            if (ALL_FIELDS.equals(field)) {
                scrollFactor += 50;
            } else {
                // TODO more sophistication needed here at some point
                scrollFactor += 0.1;
            }
        }
        if (scrollFactor < 0.1) {
            scrollFactor = 0.1;
        } else if (scrollFactor > 50) {
            scrollFactor = 50;
        }
        // apply factor to base size to get current scroll size.
        return (int) (scrollSize / scrollFactor);
    }

    /**
     * Process hits from a response using scan/scroll and write to a consumer
     * using {@link #consumeHits(Consumer, SearchResponse)}
     *
     * @param consumer destination for hits
     * @param response initial response for hit processing
     * @return current response (replaced during subsequent scrolls)
     */
    protected SearchResponse consumeAllHits(Consumer<Map<String, Object>> consumer, SearchResponse response) {
        // scroll until no hits are returned
        int n = 0;
        StopWatch watch = new StopWatch();
        while (true) {
            log.debug("Processing scroll #" + (++n));
            consumeHits(consumer, response);
            log.debug("Preparing new scroll");
            watch.reset();
            watch.start();
            response = client.prepareSearchScroll(response.getScrollId())
                    .setScroll(new TimeValue(DEFAULT_SCROLL_TIMEOUT)).execute().actionGet();
            watch.stop();
            log.debug("Prepared scroll #" + n + " in " + watch.getTime() + "ms");
            if (response.getHits().getHits().length == 0) {
                log.info("Scroll complete");
                break;
            }
        }
        return response;
    }

    /**
     * Process the current set of hits using the specified consumer
     *
     * @param consumer destination for hits
     * @param response response containing current hits
     */
    protected void consumeHits(Consumer<Map<String, Object>> consumer, SearchResponse response) {
        SearchHit[] hits = response.getHits().getHits();
        StopWatch watch = new StopWatch();
        log.debug("Processing " + hits.length + " hits");
        watch.start();
        for (SearchHit hit : hits) {
            consumer.accept(hitToMap(hit));
        }
        watch.stop();
        log.debug("Completed processing " + hits.length + " hits in " + watch.getTime() + " ms");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ensembl.genesearch.Search#query(java.util.List,
     * org.ensembl.genesearch.QueryOutput, java.util.List, int, int,
     * java.util.List)
     */
    @Override
    public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
                             List<String> sorts) {

        List<String> fieldNames = output.getPaths();

        log.debug("Building query");
        // create an elastic querybuilder object from our queries
        QueryBuilder query = ESSearchBuilder.buildQuery(type, queries.toArray(new Query[queries.size()]));

        log.info("QueryBuilder: ", query.toString());

        // prepare a search request object using the query, fields, limits etc.
        SearchRequestBuilder request = client.prepareSearch(index).setQuery(query)
                .setFetchSource(fieldNames.toArray(new String[fieldNames.size()]), null)
                .setSize(limit)
                .setFrom(offset)
                .setTypes(type);

        setFields(fieldNames, request);

        addSorts(sorts, request);

        addFacets(facets, request, DEFAULT_AGGREGATION_SIZE);

        log.info("Starting query (limit " + limit + ")");
        log.info("Query " + request.toString());

        SearchResponse response = request.execute().actionGet();
        log.info("Retrieved " + response.getHits().getHits().length + "/" + response.getHits().getTotalHits() + " in "
                + response.getTook().getMillis() + " ms");

        return new QueryResult(response.getHits().getTotalHits(), offset, limit, getFieldInfo(output),
                processResults(response), processAggregations(response));

    }

    /**
     * Set fields to retrieve based on the output specified. Note that if no
     * fields are added, only the ID will be retrieved. '*' can be used to
     * specify all fields.
     *
     * @param output
     * @param request
     */
    private void setFields(List<String> output, SearchRequestBuilder request) {
        if (output.isEmpty()) {
            request.setFetchSource(false);
        } else {
            request.setFetchSource(output.toArray(new String[output.size()]), new String[0]);
        }
    }

    /**
     * Set aggregations on a {@link SearchRequestBuilder}. Used by
     * {@link #query(List, QueryOutput, List, int, int, List)}
     *
     * @param facets
     * @param request
     */
    private void addFacets(List<String> facets, SearchRequestBuilder request, int aggregationSize) {
        for (String facet : facets) {
            log.info("Adding facet on " + facet);
            AbstractAggregationBuilder builder = ESSearchBuilder.buildAggregation(facet, aggregationSize);
            if (builder != null)
                request.addAggregation(builder);
        }
    }

    /**
     * Adds sort directives to a {@link SearchRequestBuilder} given a list of
     * field names. Used by
     * {@link #query(List, QueryOutput, List, int, int, List)}
     *
     * @param sorts   list of fields to sort on (optionally prefix with +/- to
     *                indicate sort direction)
     * @param request request to add sorts to
     */
    private void addSorts(List<String> sorts, SearchRequestBuilder request) {
        for (String sortStr : sorts) {
            Sort sort = new Sort(sortStr);
            log.info("Adding " + sort.direction + " sort on '" + sort.name + "'");
            FieldSortBuilder missing = SortBuilders.fieldSort(sort.name).order(sort.direction).missing("_last");
            if (sort.path != null) {
                missing.setNestedSort(new NestedSortBuilder(sort.path));
                // TODO support for nested filter but would need to reparse the query as QueryBuilder is not readable
            }
            request.addSort(missing);
        }
    }

    /**
     * Transform all {@link SearchHit} in a {@link SearchResponse} into a
     * {@link List} of {@link Map}s. Used by
     * {@link #query(List, QueryOutput, List, int, int, List)} where
     * scans/scroll not required.
     *
     * @param response
     * @return collection representation of hit
     */
    protected List<Map<String, Object>> processResults(SearchResponse response) {
        return Arrays.stream(response.getHits().getHits()).map(hit -> hitToMap(hit)).collect(Collectors.toList());
    }

    /**
     * Transform an instance of {@link SearchHit} into a generic {@link Map}
     *
     * @param hit ES hit to process
     * @return map representation of hit
     */
    protected Map<String, Object> hitToMap(SearchHit hit) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", hit.getId());
        if (hit.getSourceAsMap() != null)
            map.putAll(hit.getSourceAsMap());
        return map;
    }

    /**
     * Transform aggregation results from a {@link SearchResponse} into a
     * generic collection
     *
     * @param response ES response from a search
     * @return generic collection of objects.
     */
    protected Map<String, Map<String, Long>> processAggregations(SearchResponse response) {
        Map<String, Map<String, Long>> facetResults = new HashMap<>();
        if (response.getAggregations() != null) {
            for (Aggregation facet : response.getAggregations().asList()) {
                String name = getFacetName(facet, StringUtils.EMPTY);
                log.debug("Getting facet on " + name);
                Map<String, Long> facetResult = new LinkedHashMap<>();
                processAggregation(facetResult, facet);
                facetResults.put(name, facetResult);
            }
        }
        return facetResults;
    }

    /**
     * Determine the field name from an aggregation.
     *
     * @param aggregation
     * @param path
     * @return name to use for aggregation
     */
    protected String getFacetName(Aggregation aggregation, String path) {
        String aggregationName = aggregation.getName();
        if (!StringUtils.isEmpty(path)) {
            aggregationName = path + '.' + aggregationName;
        }
        if (Nested.class.isAssignableFrom(aggregation.getClass())) {
            for (Aggregation subAgg : ((Nested) aggregation).getAggregations()) {
                // note that no support for multiple nested aggs
                aggregationName = getFacetName(subAgg, aggregationName);
            }
        }
        return aggregationName;
    }

    /**
     * Process a single {@link Aggregation} object into a Map of counts keyed by
     * value
     *
     * @param facetResults
     * @param aggregation
     */
    protected void processAggregation(Map<String, Long> facetResults, Aggregation aggregation) {
        if (Terms.class.isAssignableFrom(aggregation.getClass())) {
            log.info("Processing terms aggregation " + aggregation.getName());
            for (Bucket bucket : ((Terms) aggregation).getBuckets()) {
                log.debug(bucket.getKeyAsString() + ":" + bucket.getDocCount());
                facetResults.put(bucket.getKeyAsString(), bucket.getDocCount());
            }
        } else if (Nested.class.isAssignableFrom(aggregation.getClass())) {
            for (Aggregation subAgg : ((Nested) aggregation).getAggregations()) {
                log.debug("Processing sub-aggregation " + subAgg.getName());
                processAggregation(facetResults, subAgg);
            }
        } else {
            log.warn("Cannot handle " + aggregation.getClass());
        }
    }

    /**
     * Helper to parse strings of the form +field, -field, field
     *
     * @author dstaines
     */
    private final class Sort {
        private final Pattern sortPattern = Pattern.compile("([+-])(.*)");
        public final String name;
        public final SortOrder direction;
        public final String path;

        public Sort(String str) {
            // deal with URL encoding which treats + as a space
            str = str.replaceFirst("^ ", "+");
            Matcher m = sortPattern.matcher(str);
            if (m.matches()) {
                name = m.group(2);
                direction = "+".equals(m.group(1)) ? SortOrder.ASC : SortOrder.DESC;
            } else {
                name = str;
                direction = SortOrder.ASC;
            }
            int i = name.lastIndexOf('.');
            if (i > -1) {
                path = name.substring(0, i);
            } else {
                path = null;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ensembl.genesearch.Search#fetchByIds(java.util.List,
     * java.lang.String[])
     */
    @Override
    public List<Map<String, Object>> fetchByIds(QueryOutput fields, String... ids) {
        SearchRequestBuilder request = client.prepareSearch(index)
                .setQuery(new ConstantScoreQueryBuilder(QueryBuilders.idsQuery(type).addIds(ids)));
        if (!fields.getFields().isEmpty()) {
            request.setFetchSource(fields.getFields().toArray(new String[]{}), new String[]{});
        }
        SearchResponse response = request.execute().actionGet();
        return processResults(response);
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
        SearchRequestBuilder request = client.prepareSearch(index)
                .setQuery(new ConstantScoreQueryBuilder(QueryBuilders.idsQuery(type).addIds(ids)));
        SearchResponse response = request.execute().actionGet();
        consumeAllHits(consumer, response);
        log.info("Retrieved all hits");
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
     */
    @Override
    public QueryResult select(String name, int offset, int limit) {

        QueryBuilder query;
        String[] fields;

        if (ESSearch.GENOME_ESTYPE.equals(type)) {
            query = QueryBuilders.functionScoreQuery(
                    QueryBuilders.boolQuery()
                            .should(QueryBuilders.matchPhrasePrefixQuery("organism.display_name", name).slop(10).maxExpansions(limit).boost(4))
                            .should(QueryBuilders.matchPhrasePrefixQuery("organism.scientific_name", name).slop(10).maxExpansions(limit).boost(2))
                            .should(QueryBuilders.matchPhrasePrefixQuery("organism.aliases", name).maxExpansions(limit).slop(10))
                            .should(QueryBuilders.matchPhrasePrefixQuery("organism.strain", name).maxExpansions(limit).slop(10))
                            .should(QueryBuilders.matchPhrasePrefixQuery("organism.serotype", name).maxExpansions(limit).slop(10)),
                    ScoreFunctionBuilders.fieldValueFactorFunction("is_reference").factor(2).modifier(Modifier.LOG1P));

            fields = new String[]{"id", "organism.display_name", "organism.scientific_name"};
        } else {
            // TODO check if updates needed for other types ?
            throw new UnsupportedOperationException("select not implemented for " + type);

        }

        log.info("Building query");

        log.debug(query.toString());

        SearchRequestBuilder request = client.prepareSearch(index).setQuery(query)
                .setFetchSource(fields, new String[]{}).setSize(limit).setFrom(offset)
                .setTypes(ESSearch.GENOME_ESTYPE);

        log.info("Starting query");
        log.debug("Query " + request.toString());

        SearchResponse response = request.execute().actionGet();
        log.info("Retrieved " + response.getHits().getHits().length + "/" + response.getHits().getTotalHits() + " in "
                + response.getTook().getMillis() + " ms");
        log.trace(response.toString());
        return new QueryResult(response.getHits().getTotalHits(), offset, limit,
                getFieldInfo(QueryOutput.build(Arrays.asList(fields))), processResults(response),
                processAggregations(response));

    }

    /*
     * (non-Javadoc)
     *
     * @see org.ensembl.genesearch.Search#getFieldInfo(org.ensembl.genesearch.
     * QueryOutput)
     */
    @Override
    public List<FieldInfo> getFieldInfo(QueryOutput output) {
        // ES _always_ has ID implicitly
        List<String> fieldNames = output.getFields();
        if (!fieldNames.contains(ID)) {
            fieldNames = Lists.newLinkedList(fieldNames);
            fieldNames.add(0, ID);
        }
        return Search.super.getFieldInfo(QueryOutput.build(fieldNames));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ensembl.genesearch.Search#getIdField()
     */
    @Override
    public String getIdField() {
        // use standard default ID
        return ID;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ensembl.genesearch.Search#getDataType()
     */
    @Override
    public DataTypeInfo getDataType() {
        return dataType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ensembl.genesearch.Search#up()
     */
    @Override
    public boolean up() {
        try {
            return client.admin().cluster().prepareHealth().setWaitForGreenStatus()
                    .setTimeout(TimeValue.timeValueSeconds(2)).get().getStatus().equals(ClusterHealthStatus.GREEN);
        } catch (Exception e) {
            log.warn("Could not ping ES server", e);
            return false;
        }
    }

}
