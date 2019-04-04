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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Search implementation using EVAs
 * <a href="https://www.ebi.ac.uk/eva/webservices/rest/swagger-ui.html">REST
 * implementation</a>. Very limited in field filtering and query support, so
 * most fields are queried or filtered post-retrieval.
 * 
 * This implementation uses two distinct backend services, {@link #ID_PATH} for
 * ID-based queries, and {@link #LOCATION_PATH} for range-based queries. The
 * query is analysed by {@link #getUrl(List, QueryOutput, int, int)} to
 * determine which to use for the supplied queries.
 * 
 * In addition, {@link #addExclusions(String, QueryOutput)} is used to signal to
 * the endpoint that large sub-documents are not required if the user has not
 * requested any fields found in those documents.
 * 
 * There is an absolute requirement for queries to either include an ID or a
 * location range.
 * 
 * @author dstaines
 *
 */
public class EVAVariantRestSearch extends RestBasedSearch {

    protected final static Set<String> EXCLUDE_FILTERS = new HashSet<>(Arrays.asList("sourceEntries"));
    protected final static String GENOME_FIELD = "genome";
    protected final static String ID_FIELD = "ids";
    protected final static String ID_PATH = "%s/variants/%s/info?species=%s";
    protected final static Set<String> ID_REQUEST_FILTERS = new HashSet<>(
            Arrays.asList("studies", "annot-ct", "maf", "polyphen", "sift"));
    protected final static String LOCATION_FIELD = "location";
    protected final static String LOCATION_PATH = "%s/segments/%s/variants?species=%s&limit=%d&skip=%d";
    protected final static Set<String> RANGE_REQUEST_FILTERS = new HashSet<>(
            Arrays.asList("studies", "annot-ct", "maf", "polyphen", "sift"));

    /**
     * Helper method that uses supplied queries and fields to generate a new URI
     * 
     * @param baseUri
     * @param queries
     * @param filterFields
     * @return final URI string
     */
    protected static String addFilters(String baseUri, List<Query> queries, Set<String> filterFields) {
        final StringBuilder sb = new StringBuilder(baseUri);
        queries.stream().filter(q -> filterFields.contains(q.getFieldName()))
                .forEach(q -> sb.append("&" + q.getFieldName() + "=" + StringUtils.join(q.getValues(), ',')));
        return sb.toString();
    }

    private final String baseUri;

    private int batchSize = 1000;

    private final EVAGenomeFinder finder;

    /**
     * @param baseUri
     *            Base URI of EVA REST API
     * @param info
     *            description of fields etc
     * @param finder
     *            helper to translate an Ensembl genome name into an EVA genome
     *            name
     */
    public EVAVariantRestSearch(String baseUri, DataTypeInfo info, EVAGenomeFinder finder) {
        super(info);
        this.baseUri = baseUri;
        this.finder = finder;
    }

    /**
     * EVA endpoints can exclude large chunks of documents if not required. This
     * method tests to see if any of the excludable sub-documents can be
     * excluded from the response to save bandwidth, and appends the required
     * exclusion parameters to the supplied URI
     * 
     * @param uri
     *            base URI
     * @param fieldNames
     *            list of fields needed
     * @return URI with any required filters added
     */
    protected String addExclusions(String uri, QueryOutput fieldNames) {
        List<String> excludes = new ArrayList<>(EXCLUDE_FILTERS.size());
        for (String excludePattern : EXCLUDE_FILTERS) {
            boolean found = false;
            for (String field : fieldNames.getFields()) {
                if (field.startsWith(excludePattern)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (String field : fieldNames.getSubFields().keySet()) {
                    if (field.startsWith(excludePattern)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                excludes.add(excludePattern);
            }
        }
        if (!excludes.isEmpty()) {
            uri += "&exclude=" + StringUtils.join(excludes, ',');
        }
        return uri;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.impl.RestBasedSearch#getBatchSize()
     */
    @Override
    public int getBatchSize() {
        return batchSize;
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
        List<Query> postQueries = queries.stream()
                .filter(q -> !ID_REQUEST_FILTERS.contains(q.getFieldName())
                        && !RANGE_REQUEST_FILTERS.contains(q.getFieldName()) && !ID_FIELD.equals(q.getFieldName())
                        && !GENOME_FIELD.equals(q.getFieldName()) && !LOCATION_FIELD.equals(q.getFieldName()))
                .collect(Collectors.toList());
        return postQueries;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ensembl.genesearch.impl.RestBasedSearch#getResults(com.fasterxml.
     * jackson.databind.JsonNode)
     */
    @Override
    protected JsonNode getResults(JsonNode response) {
        return response.at("/response").get(0).get("result");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.impl.RestBasedSearch#getUrl(java.util.List,
     * org.ensembl.genesearch.QueryOutput, int, int)
     */
    @Override
    protected String getUrl(List<Query> queries, QueryOutput fieldNames, int offset, int limit) {
        // determine the EVA genome name used given the supplied Ensembl genome
        // name
        Optional<Query> genome = queries.stream().filter(q -> q.getFieldName().equals(GENOME_FIELD)).findFirst();
        if (!genome.isPresent() || genome.get().getValues().length != 1) {
            throw new UnsupportedOperationException("Query must contain " + GENOME_FIELD + " with one value");
        }
        String genomeName = finder.getEVAGenomeName(genome.get().getValues()[0]);
        if (StringUtils.isEmpty(genomeName)) {
            log.debug("Queries " + queries + " did not contain a genome known by EVA");
            return StringUtils.EMPTY;
        }

        // determine whether to use ID, or range
        Optional<Query> id = queries.stream().filter(q -> q.getFieldName().equals(ID_FIELD)).findFirst();
        Optional<Query> location = queries.stream().filter(q -> q.getFieldName().equals(LOCATION_FIELD)).findFirst();
        if (id.isPresent()) {
            // use ID based path
            String uri = String.format(ID_PATH, baseUri, StringUtils.join(id.get().getValues(), ','), genomeName);
            return addExclusions(addFilters(uri, queries, ID_REQUEST_FILTERS), fieldNames);
        } else if (location.isPresent()) {
            // use range based path
            String uri = String.format(LOCATION_PATH, baseUri, StringUtils.join(location.get().getValues(), ','),
                    genomeName, limit, offset);
            return addExclusions(addFilters(uri, queries, RANGE_REQUEST_FILTERS), fieldNames);
        } else {
            throw new UnsupportedOperationException("Query must contain " + ID_FIELD + " or " + LOCATION_FIELD);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
     */
    @Override
    public QueryResult select(String name, int offset, int limit) {
        return query(Arrays.asList(new Query(FieldType.TERM, ID_FIELD, name)), null, Collections.emptyList(), offset,
                limit, Collections.emptyList());
    }

    /**
     * @param batchSize
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#up()
     */
    @Override
    public boolean up() {
        // TODO try ping to service
        return true;
    }

}
