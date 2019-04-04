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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.utils.VcfUtils;
import org.ensembl.genesearch.utils.VcfUtils.VcfFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import htsjdk.samtools.util.BufferedLineReader;

/**
 * Client class encapsulating controlled access to VCF over htsget API. Skeleton
 * support is provided for dataset and file specification but has not been
 * implemented due to a lack of a working EGA REST API, which is needed to
 * determine which files and datasets a user has access to.
 * 
 * {@link #getVariantsForFile(String, String, long, long, String, Consumer)}
 * queries based on location against each file. Results from htsget are VCF
 * files, which are parsed by {@link VcfUtils} into Map objects which are then
 * passed to a consumer.
 * 
 * @author dstaines
 *
 */
public class HtsGetClient {

    protected final static String TICKET_URL = "/data/tickets/variants/{file}?format=VCF&referenceName={referenceName}&start={start}&end={end}";

    protected final static String DATASETS_URL = "/access/v2/datasets?session={session}";
    protected final static String FILES_URL = "/access/v2/datasets/{dataset}/files?session={session}";
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected final RestTemplate restTemplate;

    protected final String baseUrl;
    protected final String egaBaseUrl;

    /**
     * @param baseUrl
     *            htsget API URL
     * @param egaBaseUrl
     *            optional EGA REST API URL for retrieving lists of files for a
     *            dataset
     */
    public HtsGetClient(String baseUrl, String egaBaseUrl) {
        this.baseUrl = baseUrl;
        this.egaBaseUrl = egaBaseUrl;
        restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);
        restTemplate.setRequestFactory(requestFactory);
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            private final ObjectMapper mapper = new ObjectMapper();
            private final TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
            };

            private Map<String, Object> parseBody(ClientHttpResponse response) throws IOException {
                return mapper.readValue(IOUtils.toString(response.getBody(), Charset.defaultCharset()), typeRef);
            }

            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                Map<String, Object> body = parseBody(response);
                log.error(response.getStatusCode() + "=>" + body.toString());
                HttpStatus statusCode = response.getStatusCode();
                switch (statusCode.series()) {
                case CLIENT_ERROR:
                    throw new HttpClientErrorException(statusCode, String.valueOf(body.get("message")));
                case SERVER_ERROR:
                    throw new HttpServerErrorException(statusCode, String.valueOf(body.get("message")));

                default:
                    throw new RestClientException("Unknown status code [" + statusCode + "]");
                }
            }
        });
    }

    /**
     * @param token
     *            oauth token
     * @return headers including bearer authentication
     */
    private HttpEntity<String> getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        return entity;
    }

    /**
     * Stub for retrieving datasets that a user has access to. Currently
     * unimplemented
     * 
     * @param session
     *            EGA session ID
     * @return list of dataset accessions
     */
    protected List<String> getDatasets(String session) {
        throw new UnsupportedOperationException();
    }

    /**
     * Obtain data URLs from htsget API
     * 
     * @param accession
     *            file accession
     * @param seqRegionName
     *            name of region e.g. 1
     * @param start
     *            region start
     * @param end
     *            region end
     * @param token
     *            oauth token
     * @return list of URLs to query
     */
    protected List<String> getUrls(String accession, String seqRegionName, long start, long end, String token) {

        if (StringUtils.isEmpty(token)) {
            throw new IllegalArgumentException("Token for htsget must be supplied");
        }

        HttpEntity<String> entity = getHeaders(token);

        ResponseEntity<JsonNode> result = restTemplate.exchange(baseUrl + TICKET_URL, HttpMethod.GET, entity,
                JsonNode.class, accession, seqRegionName, start, end);

        if (result.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Could not connect to variant server:" + result.getBody());
        }

        if (result.getBody() == null) {
            throw new RuntimeException("Could not connect to variant server - check your authentication token!");
        }

        List<String> urls = result.getBody().findValue("htsget").findValues("urls").stream()
                .map(u -> u.findValue("url").asText()).collect(Collectors.toList());
        log.info(urls.get(0));
        return urls;

    }

    /**
     * Retrieve variants for a given region (multiple datasets and files).
     * Currently unsupported.
     * 
     * @param seqRegionName
     *            name of region e.g. 1
     * @param start
     *            region start
     * @param end
     *            region end
     * @param token
     *            oauth token
     * @param session
     *            active EGA session
     * @param consumer
     *            destination for variants
     */
    public void getVariants(String seqRegionName, long start, long end, String token, String session,
            Consumer<Map<String, Object>> consumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieve variants for a given region for all files in a given dataset
     * list. Currently unsupported.
     * 
     * @param datasets
     *            accessions of datasets
     * @param seqRegionName
     *            name of region e.g. 1
     * @param start
     *            region start
     * @param end
     *            region end
     * @param token
     *            oauth token
     * @param session
     *            active EGA session
     * @param consumer
     *            destination for variants
     */
    public void getVariantsForDatasets(String[] datasets, String seqRegionName, long start, long end, String token,
            String session, Consumer<Map<String, Object>> consumer) {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieve variants for a given region in a given file.
     * 
     * @param accession
     *            file accession
     * @param seqRegionName
     *            name of region e.g. 1
     * @param start
     *            region start
     * @param end
     *            region end
     * @param token
     *            oauth token
     * @param session
     *            active EGA session
     * @param consumer
     *            destination for variants
     */
    public void getVariantsForFile(String accession, String seqRegionName, long start, long end, String token,
            Consumer<Map<String, Object>> consumer) {
        log.info(String.format("Retrieving variants from %s %s:%d-:%d", accession, seqRegionName, start, end));
        log.debug("Finding URLs");
        List<String> urls = getUrls(accession, seqRegionName, start, end, token);
        for (String url : urls) {
            log.info("Retrieving data from " + url);
            RequestCallback requestCallback = request -> request.getHeaders().add("Authorization", "Bearer " + token);
            ResponseExtractor<Void> responseExtractor = response -> {
                BufferedLineReader reader = new BufferedLineReader(response.getBody());
                VcfFormat format = VcfFormat.readFormat(reader);
                reader.lines().map(l -> VcfUtils.vcfLineToMap(l, format)).forEach(consumer);
                reader.close();
                return null;
            };
            restTemplate.execute(url, HttpMethod.GET, requestCallback, responseExtractor);

        }
        log.info("Completed retrieval");
    }

    /**
     * Retrieve variants for a given region in a given list of files.
     * 
     * @param accessions
     *            file accessions
     * @param seqRegionName
     *            name of region e.g. 1
     * @param start
     *            region start
     * @param end
     *            region end
     * @param token
     *            oauth token
     * @param session
     *            active EGA session
     * @param consumer
     *            destination for variants
     */
	public void getVariantsForFiles(String[] accessions, String seqRegionName, long start, long end, String token,
			Consumer<Map<String, Object>> consumer) {
		for (String file : accessions) {
			log.info("Retrieving variants for file " + file);
			getVariantsForFile(file, seqRegionName, start, end, token, consumer);
		}
	}

}
