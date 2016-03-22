package org.ensembl.gti.genesearch.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.gti.genesearch.services.Application;
import org.ensembl.gti.genesearch.services.GeneSearchProvider;
import org.ensembl.gti.genesearch.services.QueryParams;
import org.ensembl.gti.genesearch.services.QueryService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@WebIntegrationTest
public class QueryServiceTests {

	static Logger log = LoggerFactory.getLogger(QueryServiceTests.class);

	static QueryService queryService;

	RestTemplate restTemplate = new TestRestTemplate();

	@BeforeClass
	public static void setUp() throws IOException {
		System.out.println("Setting up");
		ESTestServer testServer = new ESTestServer();
		// index a sample of JSON
		log.info("Reading documents");
		String json = ESTestServer
				.readGzipResource("/nanoarchaeum_equitans_kin4_m.json.gz");
		log.info("Creating test index");
		testServer.createTestIndex(json);
		queryService = new QueryService(new GeneSearchProvider(
				new ESGeneSearch(testServer.getClient())));
	}

	@Test
	public void testAllQuery() {
		QueryResult result = queryService.query(new QueryParams());
		assertEquals("Checking all results found", 598, result.getResultCount());
		assertEquals("Checking limited results retrieved", 10, result
				.getResults().size());
	}

	@Test
	public void testAllQueryEndpoint() {
		String url = "http://localhost:8080/query";
		Map<String, Object> result = getUrlToMap(url);
		assertEquals("Checking all results found", 598,
				Long.parseLong(result.get("resultCount").toString()));
		assertEquals("Checking limited results retrieved", 10,
				((List<?>) result.get("results")).size());
	}

	@Test
	public void testQueryEndpoint() {
		String url = "http://localhost:8080/query"
				+ "?query={\"genome\":\"nanoarchaeum_equitans_kin4_m\"}"
				+ "&limit=5" + "&fields=name,genome" + "&sort=+name,-start"
				+ "&facets=biotype";
		Map<String, Object> result = getUrlToMap(url);
		assertEquals("Checking all results found", 598,
				Long.parseLong(result.get("resultCount").toString()));
		assertEquals("Checking limited results retrieved", 10,
				((List<?>) result.get("results")).size());
	}

	private Map<String, Object> getUrlToMap(String url) {
		ResponseEntity<String> response = restTemplate.getForEntity(url,
				String.class);
		Map<String, Object> map = null;
		try {
			map = new ObjectMapper().readValue(response.getBody(),
					new TypeReference<Map<String, Object>>() {
					});
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return map;
	}

}
