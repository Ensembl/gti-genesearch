package org.ensembl.genesearch.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;
import org.ensembl.genesearch.utils.QueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Search implementation using EVAs REST implementation
 * 
 * @author dstaines
 *
 */
public class EVAVariantRestSearch implements Search {

    protected final static String GENOME_FIELD = "genome";
    protected final static String ID_FIELD = "ids";
    protected final static String LOCATION_FIELD = "location";
    protected final static Set<String> ID_REQUEST_FILTERS = new HashSet<>(
            Arrays.asList("studies", "annot-ct", "maf", "polyphen", "sift"));
    protected final static Set<String> RANGE_REQUEST_FILTERS = new HashSet<>(
            Arrays.asList("studies", "annot-ct", "maf", "polyphen", "sift"));
    protected final static String ID_PATH = "%s/variants/%s/info?species=%s";
    protected final static String LOCATION_PATH = "%s/segments/%s/variants?species=%s&limit=%d&skip=%d";
    protected final static Set<String> EXCLUDE_FILTERS = new HashSet<>(Arrays.asList("sourceEntries"));

    private final DataTypeInfo info;
    private final String baseUri;
    private final EVAGenomeFinder finder;
    private final ObjectMapper mapper = new ObjectMapper();
    private int batchSize = 1000;
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public EVAVariantRestSearch(String baseUri, DataTypeInfo info, EVAGenomeFinder finder) {
        this.baseUri = baseUri;
        this.info = info;
        this.finder = finder;
    }

    private static String addFilters(String baseUri, List<Query> queries, Set<String> filterFields) {
        final StringBuilder sb = new StringBuilder(baseUri);
        queries.stream().filter(q -> filterFields.contains(q.getFieldName()))
                .forEach(q -> sb.append("&" + q.getFieldName() + "=" + StringUtils.join(q.getValues(), ',')));
        return sb.toString();
    }

    private String getUrl(List<Query> queries, QueryOutput fieldNames, int offset, int limit) {
        Optional<Query> genome = queries.stream().filter(q -> q.getFieldName().equals(GENOME_FIELD)).findFirst();
        if (!genome.isPresent() || genome.get().getValues().length != 1) {
            throw new UnsupportedOperationException("Query must contain " + GENOME_FIELD + " with one value");
        }
        String genomeName = finder.getEVAGenomeName(genome.get().getValues()[0]);
        if (StringUtils.isEmpty(genomeName)) {
            return StringUtils.EMPTY;
        }
        Optional<Query> id = queries.stream().filter(q -> q.getFieldName().equals(ID_FIELD)).findFirst();
        Optional<Query> location = queries.stream().filter(q -> q.getFieldName().equals(LOCATION_FIELD)).findFirst();
        if (id.isPresent()) {
            String uri = String.format(ID_PATH, baseUri, StringUtils.join(id.get().getValues(), ','), genomeName);
            return addExclusions(addFilters(uri, queries, ID_REQUEST_FILTERS), fieldNames);
        } else if (location.isPresent()) {
            String uri = String.format(LOCATION_PATH, baseUri, StringUtils.join(location.get().getValues(), ','),
                    genomeName, limit, offset - 1);
            return addExclusions(addFilters(uri, queries, RANGE_REQUEST_FILTERS), fieldNames);
        } else {
            throw new UnsupportedOperationException("Query must contain " + ID_FIELD + " or " + LOCATION_FIELD);
        }
    }

    private String addExclusions(String uri, QueryOutput fieldNames) {
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

    private JsonNode getResponse(String uri) {
        try {
            ResponseEntity<String> response = new RestTemplate().getForEntity(uri, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RestSearchException(uri, response.getBody(), response.getStatusCode());
            }
            return mapper.readTree(response.getBody()).at("/response");
        } catch (IOException e) {
            throw new RestSearchException("Could not handle response", uri, e);
        }
    }

    @SuppressWarnings("unchecked")
    protected void processResponse(Consumer<Map<String, Object>> consumer, QueryOutput fieldNames,
            List<Query> postQueries, JsonNode response) {
        // turn iterator to iterable
        Iterable<JsonNode> iterableI = () -> response.get("result").elements();

        // do the following:
        /// turn nodes into maps
        /// filter out using the post queries
        /// filter the content of the objects
        /// pass each object to the consumer
        StreamSupport.stream(iterableI.spliterator(), false)
                .map(v -> (Map<String, Object>) mapper.convertValue(v, Map.class))
                .filter(v -> QueryUtils.filterResultsByQueries.test(v, postQueries))
                .map(v -> QueryUtils.filterFields(v, fieldNames)).forEach(consumer);
    }

    protected List<Query> getPostQueries(List<Query> queries) {
        List<Query> postQueries = queries.stream()
                .filter(q -> !ID_REQUEST_FILTERS.contains(q.getFieldName())
                        && !RANGE_REQUEST_FILTERS.contains(q.getFieldName()) && !ID_FIELD.equals(q.getFieldName())
                        && !GENOME_FIELD.equals(q.getFieldName()) && !LOCATION_FIELD.equals(q.getFieldName()))
                .collect(Collectors.toList());
        return postQueries;
    }

    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
        int offset = 1;
        int resultCnt = 0;
        List<Query> postQueries = getPostQueries(queries);
        do {
            String url = getUrl(queries, fieldNames, offset, getBatchSize());
            if (url.isEmpty()) {
                log.debug("Queries "+queries+" did not contain a genome known by EVA");
                break;
            }
            JsonNode response = getResponse(url).get(0);
            if (resultCnt == 0) {
                resultCnt = Integer.parseUnsignedInt(response.get("numTotalResults").asText());
            }
            processResponse(consumer, fieldNames, postQueries, response);
            offset += getBatchSize();
        } while (resultCnt > 0 && offset <= resultCnt);
    }

    @Override
    public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
            List<String> sorts) {
        String url = getUrl(queries, output, offset, limit);
        List<Map<String, Object>> results = new ArrayList<>();
        if(url.isEmpty()) {
           log.debug("Queries "+queries+" did not contain a genome known by EVA");
        } else {
        JsonNode response = getResponse(url).get(0);        
        List<Query> postQueries = getPostQueries(queries);
        processResponse(v -> {
            results.add(v);
        }, output, postQueries, response);
        }
        return new QueryResult(-1, offset, limit, getFieldInfo(output), results, Collections.emptyMap());
    }

    @Override
    public QueryResult select(String name, int offset, int limit) {
        return query(Arrays.asList(new Query(FieldType.TERM, ID_FIELD, name)), null, Collections.emptyList(), offset,
                limit, Collections.emptyList());
    }

    @Override
    public DataTypeInfo getDataType() {
        return info;
    }

    @Override
    public boolean up() {
        // TODO
        return true;
    }

    public int getBatchSize() {
        return this.batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

}
