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
 * Search implementation using EVAs REST implementation
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

    protected static String addFilters(String baseUri, List<Query> queries, Set<String> filterFields) {
        final StringBuilder sb = new StringBuilder(baseUri);
        queries.stream().filter(q -> filterFields.contains(q.getFieldName()))
                .forEach(q -> sb.append("&" + q.getFieldName() + "=" + StringUtils.join(q.getValues(), ',')));
        return sb.toString();
    }

    private final String baseUri;

    private int batchSize = 1000;

    private final EVAGenomeFinder finder;

    public EVAVariantRestSearch(String baseUri, DataTypeInfo info, EVAGenomeFinder finder) {
        super(info);
        this.baseUri = baseUri;
        this.finder = finder;
    }

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

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    protected List<Query> getPostQueries(List<Query> queries) {
        List<Query> postQueries = queries.stream()
                .filter(q -> !ID_REQUEST_FILTERS.contains(q.getFieldName())
                        && !RANGE_REQUEST_FILTERS.contains(q.getFieldName()) && !ID_FIELD.equals(q.getFieldName())
                        && !GENOME_FIELD.equals(q.getFieldName()) && !LOCATION_FIELD.equals(q.getFieldName()))
                .collect(Collectors.toList());
        return postQueries;
    }

    @Override
    protected JsonNode getResults(JsonNode response) {
        JsonNode results = response.at("/response").get("result");
        return results.get(0);
    }

    @Override
    protected String getUrl(List<Query> queries, QueryOutput fieldNames, int offset, int limit) {
        Optional<Query> genome = queries.stream().filter(q -> q.getFieldName().equals(GENOME_FIELD)).findFirst();
        if (!genome.isPresent() || genome.get().getValues().length != 1) {
            throw new UnsupportedOperationException("Query must contain " + GENOME_FIELD + " with one value");
        }
        String genomeName = finder.getEVAGenomeName(genome.get().getValues()[0]);
        if (StringUtils.isEmpty(genomeName)) {
            log.debug("Queries " + queries + " did not contain a genome known by EVA");
            return StringUtils.EMPTY;
        }
        Optional<Query> id = queries.stream().filter(q -> q.getFieldName().equals(ID_FIELD)).findFirst();
        Optional<Query> location = queries.stream().filter(q -> q.getFieldName().equals(LOCATION_FIELD)).findFirst();
        if (id.isPresent()) {
            String uri = String.format(ID_PATH, baseUri, StringUtils.join(id.get().getValues(), ','), genomeName);
            return addExclusions(addFilters(uri, queries, ID_REQUEST_FILTERS), fieldNames);
        } else if (location.isPresent()) {
            String uri = String.format(LOCATION_PATH, baseUri, StringUtils.join(location.get().getValues(), ','),
                    genomeName, limit, offset);
            return addExclusions(addFilters(uri, queries, RANGE_REQUEST_FILTERS), fieldNames);
        } else {
            throw new UnsupportedOperationException("Query must contain " + ID_FIELD + " or " + LOCATION_FIELD);
        }
    }

    @Override
    public QueryResult select(String name, int offset, int limit) {
        return query(Arrays.asList(new Query(FieldType.TERM, ID_FIELD, name)), null, Collections.emptyList(), offset,
                limit, Collections.emptyList());
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public boolean up() {
        // TODO
        return true;
    }

}
