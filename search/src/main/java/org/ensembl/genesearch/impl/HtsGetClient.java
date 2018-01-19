package org.ensembl.genesearch.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

public class HtsGetClient {

	protected final static String TICKET_URL = "/data/tickets/variants/{file}?format=VCF&referenceName={referenceName}&start={start}&end={end}";

	protected final static String DATASETS_URL = "/access/v2/datasets?session={session}";
	protected final static String FILES_URL = "/access/v2/datasets/{dataset}/files?session={session}";
	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	protected final RestTemplate restTemplate;

	protected final String baseUrl;
	protected final String egaBaseUrl;

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

	protected List<String> getUrls(String accession, String seqRegionName, long start, long end, String token) {

		HttpEntity<String> entity = getHeaders(token);

		ResponseEntity<JsonNode> result = restTemplate.exchange(baseUrl + TICKET_URL, HttpMethod.GET, entity,
				JsonNode.class, accession, seqRegionName, start, end);

		List<String> urls = result.getBody().findValue("htsget").findValues("urls").stream()
				.map(u -> u.findValue("url").asText()).collect(Collectors.toList());
		log.info(urls.get(0));
		return urls;

	}

	public void getVariants(String seqRegionName, long start, long end, String token, String session,
			Consumer<Map<String, Object>> consumer) {
		throw new UnsupportedOperationException();
	}

	public void getVariantsForDatasets(String[] datasets, String seqRegionName, long start, long end, String token,
			String session, Consumer<Map<String, Object>> consumer) {
		throw new UnsupportedOperationException();
	}

	public void getVariantsForFile(String accession, String seqRegionName, long start, long end, String token,
			Consumer<Map<String, Object>> consumer) {
		log.info(String.format("Retrieving variants from %s %s:%d-:%d", accession, seqRegionName, start, end));
		log.debug("Finding URLs");
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
				VcfFormat format = VcfFormat.readFormat(reader);
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

	public void getVariantsForFiles(String[] accessions, String seqRegionName, long start, long end, String token,
			Consumer<Map<String, Object>> consumer) {
		for (String file : accessions) {
			log.info("Retrieving variants for file " + file);
			getVariantsForFile(file, seqRegionName, start, end, token, consumer);
		}
	}
	
	public final static void main(String[] args) {
		 HtsGetClient client = new HtsGetClient("","https://ega.ebi.ac.uk/ega/rest");
		 client.getDatasets("e74f416b-99aa-483b-8420-5f349ced0076");
	}

}
