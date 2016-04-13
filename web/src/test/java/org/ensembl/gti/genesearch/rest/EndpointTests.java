package org.ensembl.gti.genesearch.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.impl.ESGeneSearch;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.gti.genesearch.services.Application;
import org.ensembl.gti.genesearch.services.GeneSearchProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@WebIntegrationTest
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class EndpointTests {

	static Logger log = LoggerFactory.getLogger(EndpointTests.class);
	static ESGeneSearch search;

	@Autowired
	GeneSearchProvider provider;

	private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<Map<String, Object>>() {
	};
	private static final TypeReference<List<Map<String, Object>>> LIST_REF = new TypeReference<List<Map<String, Object>>>() {
	};

	RestTemplate restTemplate = new TestRestTemplate();

	@BeforeClass
	public static void setUp() throws IOException {
		// create our ES test server once only
		log.info("Setting up");
		ESTestServer testServer = new ESTestServer();
		// index a sample of JSON
		log.info("Reading documents");
		String json = ESTestServer.readGzipResource("/nanoarchaeum_equitans_kin4_m.json.gz");
		log.info("Creating test index");
		testServer.createTestIndex(json);
		search = new ESGeneSearch(testServer.getClient());
	}

	@Before
	public void injectSearch() {
		// ensure we always use our test instance
		provider.setGeneSearch(search);
	}

	@Test
	public void testQueryGetEndpoint() {
		String url = "http://localhost:8080/query";
		Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, url);
		assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
		assertEquals("Checking limited results retrieved", 10, ((List<?>) result.get("results")).size());
		List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
		assertTrue("ID found", results.get(0).containsKey("id"));
		assertTrue("Name found", results.get(0).containsKey("name"));
		assertTrue("Name found", results.get(0).containsKey("genome"));
		assertFalse("homologues not found", results.get(0).containsKey("homologues"));
	}

	@Test
	public void testQueryPostEndpoint() {
		String url = "http://localhost:8080/query";
		Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, url, "{}");
		assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
		List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
		assertEquals("Checking limited results retrieved", 10, results.size());
		assertTrue("ID found", results.get(0).containsKey("id"));
		assertFalse("Name found", results.get(0).containsKey("name"));
		assertFalse("Genome found", results.get(0).containsKey("genome"));
		assertFalse("Homologues not found", results.get(0).containsKey("homologues"));
		Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
		assertTrue("Checking no facets retrieved", facets.isEmpty());
	}

	@Test
	public void testFullQueryGetEndpoint() {
		String url = "http://localhost:8080/query" + "?query={query}" + "&limit=5" + "&fields=name,description"
				+ "&sort=+name,-start" + "&facets=biotype";
		// rest template expands {} as variables so supply JSON separately
		Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, url,
				"{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
		assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
		List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
		assertEquals("Checking limited results retrieved", 5, results.size());
		assertTrue("ID found", results.get(0).containsKey("id"));
		assertTrue("Name found", results.get(0).containsKey("name"));
		assertTrue("Description found", results.get(0).containsKey("description"));
		assertFalse("homologues not found", results.get(0).containsKey("homologues"));
		Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
		assertEquals("Checking 1 facet retrieved", 1, facets.size());
		assertTrue("Checking facets populated", facets.containsKey("biotype"));
		assertEquals("Name found", "5S_rRNA", results.get(0).get("name"));
	}

	@Test
	public void testFullQueryPostEndpoint() {
		String paramJson = "{\"query\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"},"
				+ "\"limit\":5,\"fields\":[\"name\",\"genome\",\"description\"]," + "\"sort\":[\"+name\",\"-start\"],"
				+ "\"facets\":[\"biotype\"]}";
		// rest template expands {} as variables so supply JSON separately
		Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, "http://localhost:8080/query", paramJson);
		assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
		List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
		assertEquals("Checking limited results retrieved", 5, results.size());
		assertTrue("ID found", results.get(0).containsKey("id"));
		assertTrue("Name found", results.get(0).containsKey("name"));
		assertTrue("Description found", results.get(0).containsKey("description"));
		assertFalse("homologues not found", results.get(0).containsKey("homologues"));
		Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
		assertEquals("Checking 1 facet retrieved", 1, facets.size());
		assertTrue("Checking facets populated", facets.containsKey("biotype"));
		assertEquals("Name found", "5S_rRNA", results.get(0).get("name"));
	}

	@Test
	public void testFetchGetEndpoint() {
		String url = "http://localhost:8080/fetch";
		List<Map<String, Object>> result = getUrlToObject(LIST_REF, restTemplate, url);
		assertEquals("Checking all results found", 598, result.size());
		assertTrue("ID found", result.get(0).containsKey("id"));
		assertTrue("Name found", result.get(0).containsKey("name"));
		assertTrue("Description found", result.get(0).containsKey("description"));
		assertFalse("Homologues found", result.get(0).containsKey("homologues"));
		assertFalse("Transcripts found", result.get(0).containsKey("transcripts"));
	}

	@Test
	public void testFetchPostEndpoint() {
		String url = "http://localhost:8080/fetch";
		List<Map<String, Object>> result = postUrlToObject(LIST_REF, restTemplate, url, "{}");
		assertEquals("Checking all results found", 598, result.size());
		assertTrue("ID found", result.get(0).containsKey("id"));
		assertTrue("Name found", result.get(0).containsKey("name"));
		assertTrue("Description found", result.get(0).containsKey("description"));
		// assertFalse("homologues not found",
		// result.get(0).containsKey("homologues"));
	}

	@Test
	public void testFullFetchGetEndpoint() {
		String url = "http://localhost:8080/fetch" + "?query={query}" + "&fields=name,start";
		// rest template expands {} as variables so supply JSON separately
		List<Map<String, Object>> result = getUrlToObject(LIST_REF, restTemplate, url,
				"{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
		assertEquals("Checking all results found", 598, result.size());
		assertTrue("ID found", result.get(0).containsKey("id"));
		assertTrue("Name found", result.get(0).containsKey("name"));
		assertTrue("Start found", result.get(0).containsKey("start"));
		assertFalse("homologues not found", result.get(0).containsKey("homologues"));
	}

	@Test
	public void testFullFetchPostEndpoint() {
		String paramJson = "{\"query\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"},"
				+ "\"fields\":[\"name\",\"genome\",\"start\"]}";
		// rest template expands {} as variables so supply JSON separately
		List<Map<String, Object>> result = postUrlToObject(LIST_REF, restTemplate, "http://localhost:8080/fetch",
				paramJson);
		assertEquals("Checking all results found", 598, result.size());
		assertTrue("ID found", result.get(0).containsKey("id"));
		assertTrue("Name found", result.get(0).containsKey("name"));
		assertTrue("Start found", result.get(0).containsKey("start"));
		assertFalse("homologues not found", result.get(0).containsKey("homologues"));
	}

	/**
	 * Helper method for invoking a URI as GET and parsing the result into a
	 * hash
	 * 
	 * @param url
	 *            URL template
	 * @param params
	 *            bind parameters for URL
	 * @return
	 */
	public static <T> T getUrlToObject(TypeReference<T> type, RestTemplate restTemplate, String url, Object... params) {
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, params);
		log.info("Get response: " + response.getBody());
		T map = null;
		try {
			map = new ObjectMapper().readValue(response.getBody(), type);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return map;
	}

	/**
	 * Helper method to invoke a JSON POST method with the supplied object and
	 * then return the resulting object
	 * 
	 * @param restTemplate
	 * @param url
	 *            URL
	 * @param json
	 *            object to post
	 * @param params
	 *            URL bind params
	 * @return
	 */
	public static <T> T postUrlToObject(TypeReference<T> type, RestTemplate restTemplate, String url, String json,
			Object... params) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			log.trace("Invoking " + url + " with " + json);
			HttpEntity<String> entity = new HttpEntity<String>(json, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
			log.trace("Post response: " + response.getBody());
			return new ObjectMapper().readValue(response.getBody(), type);

		} catch (IOException e) {
			fail(e.getMessage());
			return null;
		}

	}

}
