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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.QueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Abstract class that can be extended to use a generic REST backend. Assumes that
 * fields can be classified as either URI or post-retrieval. Provides template
 * methods for transforming a URI response into a JsonNode document.
 * 
 * @author dstaines
 *
 */
public abstract class RestBasedSearch implements Search {

    protected final DataTypeInfo info;
    protected final Logger log = LoggerFactory.getLogger(RestBasedSearch.class);
    protected final ObjectMapper mapper = new ObjectMapper();

    public RestBasedSearch(DataTypeInfo info) {
        this.info = info;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
     * java.util.List, org.ensembl.genesearch.QueryOutput)
     */
    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
        int offset = 0;
        int resultCnt = 0;
        List<Query> postQueries = getPostQueries(queries);
        do {
            String url = getUrl(queries, fieldNames, offset, getBatchSize());
            if (url.isEmpty()) {
                break;
            }
            log.info("Executing fetch");
            JsonNode response = getResponse(url);
            if (resultCnt == 0) {
                resultCnt = Integer.parseUnsignedInt(response.get("response").get(0).get("numTotalResults").asText());
            }
            processResponse(consumer, fieldNames, postQueries, response);
            log.info("Fetch executed");
            offset += getBatchSize();
        } while (resultCnt > 0 && offset < resultCnt);
    }

    /**
     * @return number of documents to retrieve at one time
     */
    public abstract int getBatchSize();

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#getDataType()
     */
    public DataTypeInfo getDataType() {
        return info;
    }

    /**
     * @param queries
     *            all queries
     * @return Find supplied queries that must be applied post-retrieval
     */
    protected abstract List<Query> getPostQueries(List<Query> queries);

    /**
     * Invoke URI and parse body of response as JSON using
     * {@link #getResults(JsonNode)}
     * 
     * @param uri
     * @return body
     */
    protected JsonNode getResponse(String uri) {
        try {
            log.info("Querying " + uri);
            ResponseEntity<String> response = new RestTemplate().getForEntity(uri, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RestSearchException(uri, response.getBody(), response.getStatusCode());
            }
            log.info("Response retrieved");
            return mapper.readTree(response.getBody());
        } catch (IOException e) {
            throw new RestSearchException("Could not handle response", uri, e);
        }
    }

    /**
     * Transform a response into a JSON document
     * 
     * @param response
     * @return
     */
    protected abstract JsonNode getResults(JsonNode response);

    /**
     * Generate a URL from the supplied queries, fields and pagination options
     * 
     * @param queries
     * @param fieldNames
     * @param offset
     * @param limit
     * @return URL to invoke
     */
    protected abstract String getUrl(List<Query> queries, QueryOutput fieldNames, int offset, int limit);

    /**
     * Process a standard response and pass results to consumer. Uses template methods consumeStream and getResults
     * 
     * @param consumer
     * @param fieldNames
     * @param postQueries
     * @param response
     */
    protected void processResponse(Consumer<Map<String, Object>> consumer, QueryOutput fieldNames,
            List<Query> postQueries, JsonNode response) {
        consumeStream(consumer, fieldNames, postQueries, resultsToStream(getResults(response)));
    }

    /**
     * Utility to transform results JsonNode into a stream for further processing e.g. by stream functions or a consumer
     * 
     * @param results
     * @return
     */
    protected static Stream<JsonNode> resultsToStream(JsonNode results) {

        if (results != null && results.size() > 0) {
            // turn iterator to iterable
            Iterable<JsonNode> iterableI = () -> results.elements();
            return StreamSupport.stream(iterableI.spliterator(), false);
        } else {
            return Stream.empty();
        }
    }

    /**
     * Filter and process a stream before passing to a consumer
     * 
     * @param consumer
     * @param fieldNames
     * @param postQueries
     * @param stream
     */
    @SuppressWarnings("unchecked")
    protected void consumeStream(Consumer<Map<String, Object>> consumer, QueryOutput fieldNames,
            List<Query> postQueries, Stream<JsonNode> stream) {

        // do the following:
        /// turn nodes into maps
        /// filter out using the post queries
        /// filter the content of the objects
        /// pass each object to the consumer
        stream.map(v -> (Map<String, Object>) mapper.convertValue(v, Map.class))
                .filter(v -> QueryUtils.filterResultsByQueries.test(v, postQueries))
                .map(v -> QueryUtils.filterFields(v, fieldNames)).forEach(consumer);
    }

    /* (non-Javadoc)
     * @see org.ensembl.genesearch.Search#query(java.util.List, org.ensembl.genesearch.QueryOutput, java.util.List, int, int, java.util.List)
     */
    public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
            List<String> sorts) {
        String url = getUrl(queries, output, offset, limit);
        List<Map<String, Object>> results = new ArrayList<>();
        if (!url.isEmpty()) {
            log.info("Executing query");
            JsonNode response = getResponse(url);
            log.info("Processing query");
            List<Query> postQueries = getPostQueries(queries);
            processResponse(v -> {
                results.add(v);
            }, output, postQueries, response);
            log.info(results.size() + " results retrieved");
        }
        return new QueryResult(-1, offset, limit, getFieldInfo(output), results, Collections.emptyMap());
    }

}