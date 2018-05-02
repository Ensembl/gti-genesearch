package org.ensembl.genesearch.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.ensembl.genesearch.utils.VcfUtils;
import org.ensembl.genesearch.utils.VcfUtils.VcfFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

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
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
    }

    private HttpEntity<String> getHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        return entity;
    }

    protected List<String> getDatasets(String session) {
        ResponseEntity<JsonNode> result = restTemplate.getForEntity(egaBaseUrl + DATASETS_URL, JsonNode.class, session);
        return null;
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

        HttpEntity<String> entity = getHeaders(token);

        ResponseEntity<JsonNode> result = restTemplate.exchange(baseUrl + TICKET_URL, HttpMethod.GET, entity,
                JsonNode.class, accession, seqRegionName, start, end);

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
        // get a list of data URLs for this file and region
        List<String> urls = getUrls(accession, seqRegionName, start, end, token);
        HttpEntity<String> entity = getHeaders(token);
        for (String url : urls) {
            log.info("Retrieving data from " + url);
            ResponseEntity<Resource> result = restTemplate.exchange(url, HttpMethod.GET, entity, Resource.class);
            if (result.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException(url + " retrieved with code " + result.getStatusCode());
            }
            BufferedLineReader reader = null;
            try {
                reader = new BufferedLineReader(result.getBody().getInputStream());
                // retrieve VCF format to support later parsing
                VcfFormat format = VcfFormat.readFormat(reader);
                // parse variants
                reader.lines().map(l -> VcfUtils.vcfLineToMap(l, format)).forEach(consumer);
                reader.lines().forEach(System.out::println);
            } catch (IOException e) {
                throw new RuntimeException("Could not read from result", e);
            } finally {
                IOUtils.closeQuietly(reader);
            }

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
