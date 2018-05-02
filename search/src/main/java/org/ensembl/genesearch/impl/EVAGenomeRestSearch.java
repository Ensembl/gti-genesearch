package org.ensembl.genesearch.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.QueryUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Search implementation using EVAs REST implementation for finding lists of
 * genomes they support
 * 
 * Lazily loads entire genome set (which is currently very small) using
 * {@link #getGenomes()} for subsequent in-memory query
 * 
 * @author dstaines
 *
 */
public class EVAGenomeRestSearch implements Search {

    private final static String SPECIES_PATH = "/meta/species/list";

    private final DataTypeInfo info;
    private final String baseUri;
    private List<Map<String, Object>> genomes;

    public EVAGenomeRestSearch(String baseUri, DataTypeInfo info) {
        this.baseUri = baseUri;
        this.info = info;
    }

    /**
     * @return
     */
    protected List<Map<String, Object>> getGenomes() {
        if (genomes == null) {
            String uri = baseUri + "/" + SPECIES_PATH;
            try {
                ResponseEntity<String> response = new RestTemplate().getForEntity(uri, String.class);
                if (response.getStatusCode() != HttpStatus.OK) {
                    throw new RestSearchException(uri, response.getBody(), response.getStatusCode());
                }
                ObjectMapper mapper = new ObjectMapper();
                JsonNode at = mapper.readTree(response.getBody()).at("/response").get(0).get("result");
                genomes = StreamSupport.stream(at.spliterator(), false)
                        .map(n -> (Map<String, Object>) mapper.convertValue(n, Map.class)).collect(Collectors.toList());
            } catch (IOException e) {
                throw new RestSearchException("Could not handle response", uri, e);
            }
        }
        return genomes;
    }

    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
        getGenomes().stream().filter(o -> QueryUtils.filterResultsByQueries.test(o, queries)).map(o -> {
            QueryUtils.filterFields(o, fieldNames);
            return o;
        }).forEach(consumer);
    }

    @Override
    public QueryResult query(List<Query> queries, QueryOutput fieldNames, List<String> facets, int offset, int limit,
            List<String> sorts) {
        if (sorts != null && !sorts.isEmpty()) {
            throw new UnsupportedOperationException("Sorting not supported for " + getDataType().getName());
        }
        if (facets != null && !facets.isEmpty()) {
            throw new UnsupportedOperationException("Faceting not supported for " + getDataType().getName());
        }
        List<Map<String, Object>> results = getGenomes().stream()
                .filter(o -> QueryUtils.filterResultsByQueries.test(o, queries)).skip(offset - 1).limit(limit)
                .map(o -> {
                    QueryUtils.filterFields(o, fieldNames);
                    return o;
                }).collect(Collectors.toList());
        return new QueryResult(-1, offset, limit, getFieldInfo(fieldNames), results, null);
    }

    @Override
    public QueryResult select(String name, int offset, int limit) {
        List<Map<String, Object>> genomes = getGenomes().stream().filter(g -> name.equals(g.get("assemblyAccession")))
                .collect(Collectors.toList());
        return new QueryResult(genomes.size(), offset, limit, getDataType().getFieldInfo(), genomes,
                Collections.emptyMap());
    }

    @Override
    public DataTypeInfo getDataType() {
        return info;
    }

    @Override
    public boolean up() {
        // TODO if possible
        return true;
    }

}
