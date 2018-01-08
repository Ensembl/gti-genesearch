package org.ensembl.genesearch.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.ensembl.genesearch.utils.VcfUtils;
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

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	protected final static String TICKET_URL = "/data/tickets/variants/{file}?format=VCF&referenceName={referenceName}&start={start}&end={end}";

	protected final RestTemplate restTemplate;

	protected final String baseUrl;

	public HtsGetClient(String baseUrl) {
		this.baseUrl = baseUrl;
		restTemplate = new RestTemplate();
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
	}

	public void getVariants(String accession, String seqRegionName, long start, long end, String token,
			Consumer<Map<String, Object>> consumer) {
		log.info(String.format("Retrieving variants from %s %s:%d-:%d", accession, seqRegionName, start, end));
		log.debug("Finding URLs");
		List<String> urls = getUrls(accession, seqRegionName, start, end, token);
		HttpEntity<String> entity = getHeaders(token);
		for (String url : urls) {
			log.info("Retrieving data from "+url);
			ResponseEntity<Resource> result = restTemplate.exchange(url, HttpMethod.GET, entity,
					Resource.class);
			if(result.getStatusCode()!=HttpStatus.OK) {
				throw new RuntimeException(url+" retrieved with code "+result.getStatusCode());
			}
			BufferedLineReader reader = null;
			try {
			reader = new BufferedLineReader(result.getBody().getInputStream());
			
			Optional<String> colsLine = reader.lines().filter(VcfUtils.isColsLine()).findFirst();
			if(!colsLine.isPresent()) {
				throw new RuntimeException("No column header line returned by "+url);
			}
			String[] genotypes = VcfUtils.getGenotypes(colsLine.get());
			reader.lines().map(l -> VcfUtils.vcfLineToMap(l, genotypes)).forEach(consumer);
			reader.lines().forEach(System.out::println);
			} catch (IOException e) {
				throw new RuntimeException("Could not read from result",e);
			} finally {
				IOUtils.closeQuietly(reader);
			}
			
		}
		log.info("Completed retrieval");
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

	private HttpEntity<String> getHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		return entity;
	}

}
