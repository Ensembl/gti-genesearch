package org.ensembl.genesearch.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class that can be extended to use a generic REST backend
 * @author dstaines
 *
 */
public abstract class RestBasedSearch implements Search {

    protected final DataTypeInfo info;
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    protected final ObjectMapper mapper = new ObjectMapper();

    public RestBasedSearch(DataTypeInfo info) {
        this.info = info;
    }

    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
        int offset = 1;
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
                resultCnt = Integer.parseUnsignedInt(response.get("numTotalResults").asText());
            }
            processResponse(consumer, fieldNames, postQueries, response);
            log.info("Fetch executed");
            offset += getBatchSize();
        } while (resultCnt > 0 && offset <= resultCnt);
    }

    public abstract int getBatchSize();

    
    public DataTypeInfo getDataType() {
        return info;
    }

    protected abstract List<Query> getPostQueries(List<Query> queries);
    
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

    protected abstract JsonNode getResults(JsonNode response);
    
    protected abstract String getUrl(List<Query> queries, QueryOutput fieldNames, int offset, int limit);
    
    /**
     * Process a standard response
     * @param consumer
     * @param fieldNames
     * @param postQueries
     * @param response
     */
    protected void processResponse(Consumer<Map<String, Object>> consumer, QueryOutput fieldNames,
            List<Query> postQueries, JsonNode response) {
        consumeStream(consumer, fieldNames,
                postQueries, resultsToStream(getResults(response)));
    }
    
    /**
     * Transform results jsonnode into a stream for further processing
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
        stream
                .map(v -> (Map<String, Object>) mapper.convertValue(v, Map.class))
                .filter(v -> QueryUtils.filterResultsByQueries.test(v, postQueries))
                .map(v -> QueryUtils.filterFields(v, fieldNames)).forEach(consumer);
    }
    
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