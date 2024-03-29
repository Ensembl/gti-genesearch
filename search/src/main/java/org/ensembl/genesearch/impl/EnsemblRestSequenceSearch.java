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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.CollectionUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.RestTemplate;

/**
 * {@link Search} using an Ensembl-style REST API to retrieve sequences
 * 
 * @author dstaines
 *
 */
public class EnsemblRestSequenceSearch implements Search {

    private static final String MULTIPLE_SEQUENCES = "multiple_sequences=1";

    /**
     * Magic field which contains the query ID to which the sequences belong.
     * Vital for joining. Used by {@link #getIds(List)} to extract the query
     */
    public static final String QUERY = "query";

    private static final int CONNECT_TIMEOUT = 5000;

    /**
     * valid arguments to pass as fields. Used by {@link #getPostUrl(List)}
     */
    public final static List<String> VALID_ARGS = Arrays.asList("type", "expand_5prime", "expand_3prime", "type",
            "format", "species");

    public final static int DEFAULT_BATCH_SIZE = 50;

    public final static String SEQUENCE_ID = "/sequence/id";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final String baseUrl;
    private final int batchSize;
    private final RestTemplate template = new RestTemplate();
    private final DataTypeInfo dataType;

    /**
     * @param baseUrl
     *            base URL of REST API
     * @param dataType
     *            type to use as label for search
     * @param batchSize
     *            number of sequences to retrieve at one time
     */
    public EnsemblRestSequenceSearch(String baseUrl, DataTypeInfo dataType, int batchSize) {
        this.baseUrl = baseUrl;
        this.batchSize = batchSize;
        this.dataType = dataType;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        if (!StringUtils.isEmpty(proxyHost)) {
            int port = 80;
            if (!StringUtils.isEmpty(proxyPort)) {
                port = Integer.valueOf(proxyPort);
            }
            log.info("Using proxy " + proxyHost + ":" + proxyPort);
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, port)));
        } else {
            log.info("No proxy used");
        }
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        template.setRequestFactory(factory);
    }

    /**
     * @param baseUrl
     *            base URL of REST API
     * @param dataType
     *            type to use as label for search
     */
    public EnsemblRestSequenceSearch(String baseUrl, DataTypeInfo dataType) {
        this(baseUrl, dataType, DEFAULT_BATCH_SIZE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
     * java.util.List, org.ensembl.genesearch.QueryOutput)
     */
    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {

        // transform the query string into a base URI
        String url = getPostUrl(queries);
        log.info("Using base URL " + url);
        // extract IDs from supplied queries
        List<String> ids = getIds(queries);
        log.info("Searching for " + ids.size() + " ids");
        // work through IDs in batches (REST server currently only allows 50 IDs
        // at a time)
        Map<String, Object> idParams = new HashMap<>();
        int n = 0;
        for (List<String> idList : CollectionUtils.eagerPartition(ids, batchSize)) {
            idParams.put("ids", idList);
            n += idList.size();
            // pass sequences to consumer
            log.debug("Posting " + idList.size() + " IDs to " + url);
            try {
                template.postForObject(url, idParams, List.class).stream().forEach(consumer);
            } catch (HttpMessageNotReadableException e) {
                log.warn("Could not find sequences for " + idList);
            }
        }
        log.info("Completed querying " + n + " IDs");

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#getFieldInfo(org.ensembl.genesearch.
     * QueryOutput)
     */
    @Override
    public List<FieldInfo> getFieldInfo(QueryOutput fields) {
        return getDataType().getFieldInfo();
    }

    /**
     * Extract any query params that need to be passed to the REST service,
     * using VALID_ARGS as a lookup (allows support for molecule type, end
     * extension etc). This provides a URL that can have IDs POSTed to it.
     * 
     * @param queries
     * @return base URL
     */
    protected String getPostUrl(List<Query> queries) {
        // find which queries are valid given our standard list
        List<String> params = queries.stream()
                .filter(q -> VALID_ARGS.contains(q.getFieldName()) && q.getValues().length == 1)
                .map(q -> q.getFieldName() + "=" + q.getValues()[0]).collect(Collectors.toList());
        // add a magic query param to indicate we want multiple queries
        params.add(MULTIPLE_SEQUENCES);
        // generate a parameterised URL to POST to
        return baseUrl + SEQUENCE_ID + "?" + StringUtils.join(params, '&');

    }

    /**
     * extract IDs from the queries
     * 
     * @param queries
     * @return IDs as list
     */
    protected List<String> getIds(List<Query> queries) {
        return queries.stream().filter(q -> q.getFieldName().equals(getIdField()) || q.getFieldName().equals(QUERY))
                .flatMap(q -> Arrays.asList(q.getValues()).stream()).collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#fetchByIds(java.util.List,
     * java.lang.String[])
     */
    @Override
    public List<Map<String, Object>> fetchByIds(QueryOutput fields, String... ids) {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
        // use ping endpoint to determine if the underlying REST API is working
        String url = baseUrl.replace("/sequence/id", "/info/ping");
        try {
            ResponseEntity<Map> response = template.getForEntity(url, Map.class);
            if (!response.getStatusCode().equals(HttpStatus.OK)) {
                log.warn(url + " returns status " + response.getStatusCode());
            }
            String ping = String.valueOf(response.getBody().get("ping"));
            if (!ping.equals("1")) {
                log.warn(url + " returns ping " + ping);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("Cannot check status from " + url, e);
            return false;
        }
    }

}
