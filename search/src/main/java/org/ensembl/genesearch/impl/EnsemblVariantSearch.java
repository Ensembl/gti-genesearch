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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.QueryUtils;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * REST-based {@link Search} using the Ensembl REST API to retrieve variants.
 * The REST API is limited to querying by ID, genome or location. All other
 * fields are applied as post-retrieval filters. fetch only supports locations.
 * 
 * @author dstaines
 *
 */
public class EnsemblVariantSearch extends RestBasedSearch {

    final static String GENOME_FIELD = "genome";
    protected final static String LOCATION_FIELD = "location";
    protected final static String LOCATION_PATH = "%s/overlap/region/%s/%s?feature=variation;"
            + "feature=somatic_variation;feature=structural_variation;feature=somatic_structural_variation;"
            + "content-type=application/json";
    protected final static String ID_FIELD = "id";
    protected final static String ID_PATH = "%s/variation/%s/%s?content-type=application/json";
    private final String baseUri;

    public EnsemblVariantSearch(String baseUri, DataTypeInfo info) {
        super(info);
        this.baseUri = baseUri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
     */
    @Override
    public QueryResult select(String name, int offset, int limit) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#up()
     */
    @Override
    public boolean up() {
        // could also use ping here
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.impl.RestBasedSearch#getBatchSize()
     */
    @Override
    public int getBatchSize() {
        return 100000;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.impl.RestBasedSearch#query(java.util.List,
     * org.ensembl.genesearch.QueryOutput, java.util.List, int, int,
     * java.util.List)
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.impl.RestBasedSearch#query(java.util.List,
     * org.ensembl.genesearch.QueryOutput, java.util.List, int, int,
     * java.util.List)
     */
    @Override
    public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
            List<String> sorts) {
        
        // we override this method to allow the imposition of offset/limit at
        // the stream level
        if (facets != null && !facets.isEmpty()) {
            throw new UnsupportedOperationException("Faceting not supported");
        }
        if (sorts != null && !sorts.isEmpty()) {
            throw new UnsupportedOperationException("Sorting not supported");
        }

        String url = getUrl(queries, output, offset, limit);
        List<Map<String, Object>> results = new ArrayList<>();
        if (!url.isEmpty()) {
            log.info("Executing query");
            JsonNode response = getResponse(url);
            log.info("Processing query");
            List<Query> postQueries = getPostQueries(queries);

            // filter stream, convert, offset
            resultsToStream(getResults(response)).map(v -> (Map<String, Object>) mapper.convertValue(v, Map.class))
                    .filter(v -> QueryUtils.filterResultsByQueries.test(v, postQueries)).skip(offset).limit(limit)
                    .map(v -> QueryUtils.filterFields(v, output)).forEach(v -> results.add(v));

            log.info(results.size() + " results retrieved");
        }
        return new QueryResult(-1, offset, limit, getFieldInfo(output), results, Collections.emptyMap());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ensembl.genesearch.impl.RestBasedSearch#fetch(java.util.function.
     * Consumer, java.util.List, org.ensembl.genesearch.QueryOutput)
     */
    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
        // location based fetch only
        Optional<Query> location = queries.stream().filter(q -> q.getFieldName().equals(LOCATION_FIELD)).findFirst();
        if (!location.isPresent()) {
            throw new UnsupportedOperationException("Fetch currently only supports location based queries");
        }
        Optional<Query> genome = queries.stream().filter(q -> q.getFieldName().equals(GENOME_FIELD)).findFirst();
        List<Query> postQueries = getPostQueries(queries);
        for (String locStr : location.get().getValues()) {
            log.info("Executing fetch for location " + locStr);
            Matcher m = Query.LOCATION.matcher(locStr);
            if (m.matches()) {
                String name = m.group(1);
                long start = Long.valueOf(m.group(2));
                long end = Long.valueOf(m.group(3));
                long i = start;
                while (i < end) {
                    long j = Math.min(end, i + getBatchSize());
                    String subLoc = name + ":" + i + "-" + j;
                    log.info("Executing fetch for sub-location " + subLoc);
                    String url = String.format(LOCATION_PATH, baseUri, genome.get().getValues()[0], subLoc);
                    log.info("Executing fetch");
                    JsonNode response = getResponse(url);
                    processResponse(consumer, fieldNames, postQueries, response);
                    i = j + 1;
                }
            } else {
                throw new IllegalArgumentException("Location " + locStr
                        + " does not match the expected location format: " + Query.LOCATION.pattern());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ensembl.genesearch.impl.RestBasedSearch#getPostQueries(java.util.
     * List)
     */
    @Override
    protected List<Query> getPostQueries(List<Query> queries) {
        // all queries apart from ID, genome and location are applied in post
        List<Query> postQueries = queries.stream().filter(q -> !ID_FIELD.equals(q.getFieldName())
                && !GENOME_FIELD.equals(q.getFieldName()) && !LOCATION_FIELD.equals(q.getFieldName()))
                .collect(Collectors.toList());
        return postQueries;
    }

    /* (non-Javadoc)
     * @see org.ensembl.genesearch.impl.RestBasedSearch#getResults(com.fasterxml.jackson.databind.JsonNode)
     */
    @Override
    protected JsonNode getResults(JsonNode response) {
        return response;
    }

    /* (non-Javadoc)
     * @see org.ensembl.genesearch.impl.RestBasedSearch#getUrl(java.util.List, org.ensembl.genesearch.QueryOutput, int, int)
     */
    @Override
    protected String getUrl(List<Query> queries, QueryOutput fieldNames, int offset, int limit) {
        Optional<Query> genome = queries.stream().filter(q -> q.getFieldName().equals(GENOME_FIELD)).findFirst();
        if (!genome.isPresent() || genome.get().getValues().length != 1) {
            throw new UnsupportedOperationException("Query must contain " + GENOME_FIELD + " with one value");
        }
        String genomeName = genome.get().getValues()[0];
        Optional<Query> id = queries.stream().filter(q -> q.getFieldName().equals(ID_FIELD)).findFirst();
        Optional<Query> location = queries.stream().filter(q -> q.getFieldName().equals(LOCATION_FIELD)).findFirst();
        if (id.isPresent()) {
            String uri = String.format(ID_PATH, baseUri, genomeName, id.get().getValues()[0]);
            return uri;
        } else if (location.isPresent()) {
            String uri = String.format(LOCATION_PATH, baseUri, genomeName, location.get().getValues()[0]);
            return uri;
        } else {
            throw new UnsupportedOperationException("Query must contain " + ID_FIELD + " or " + LOCATION_FIELD);
        }
    }

}
