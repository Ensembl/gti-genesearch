package org.ensembl.genesearch.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
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
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setBufferRequestBody(false);
		restTemplate.setRequestFactory(requestFactory);
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
	}

	private HttpEntity<String> getHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		return entity;
	}

	protected List<String> getDatasets(String session) {
		throw new UnsupportedOperationException();
	}

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

	public void getVariantsForFiles(String[] accessions, String seqRegionName, long start, long end, String token,
			Consumer<Map<String, Object>> consumer) {
		for (String file : accessions) {
			log.info("Retrieving variants for file " + file);
			getVariantsForFile(file, seqRegionName, start, end, token, consumer);
		}
	}

}
