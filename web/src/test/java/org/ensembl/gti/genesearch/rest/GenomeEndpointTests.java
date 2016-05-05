/*
 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ensembl.gti.genesearch.rest;

import static org.ensembl.gti.genesearch.rest.GeneEndpointTests.getUrlToObject;
import static org.ensembl.gti.genesearch.rest.GeneEndpointTests.postUrlToObject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.gti.genesearch.services.Application;
import org.ensembl.gti.genesearch.services.SearchProvider;
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
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@WebIntegrationTest
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class GenomeEndpointTests {

	private static final String GENOMES_FETCH = "http://localhost:8080/api/genomes/fetch";
	private static final String GENOMES_QUERY = "http://localhost:8080/api/genomes/query";
	private static final String GENOMES_SELECT = "http://localhost:8080/api/genomes/select";
	
	static Logger log = LoggerFactory.getLogger(GenomeEndpointTests.class);
	static ESSearch search;

	@Autowired
	SearchProvider provider;

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
		String json = ESTestServer.readGzipResource("/genomes.json.gz");
		log.info("Creating test index");
		testServer.createTestIndex(json, ESSearch.GENES_INDEX, ESSearch.GENOME_TYPE);
		search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENOME_TYPE);
	}

	@Before
	public void injectSearch() {
		// ensure we always use our test instance
		provider.setGenomeSearch(search);
	}

	@Test
	public void testQueryGetEndpoint() {
		Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, GENOMES_QUERY);
		assertEquals("Checking all results found", 4, Long.parseLong(result.get("resultCount").toString()));
		assertEquals("Checking limited results retrieved", 4, ((List<?>) result.get("results")).size());
		List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
		assertTrue("ID found", results.get(0).containsKey("id"));
	}

	@Test
	public void testQueryPostEndpoint() {
		Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, GENOMES_QUERY, "{}");
		assertEquals("Checking all results found", 4, Long.parseLong(result.get("resultCount").toString()));
		List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
		assertEquals("Checking limited results retrieved", 4, results.size());
		assertTrue("ID found", results.get(0).containsKey("id"));
	}

	@Test
	public void testFetchGetEndpoint() {
		List<Map<String, Object>> result = getUrlToObject(LIST_REF, restTemplate, GENOMES_FETCH);
		assertEquals("Checking all results found", 4, result.size());
		assertTrue("ID found", result.get(0).containsKey("id"));
	}

	@Test
	public void testFetchPostEndpoint() {
		List<Map<String, Object>> result = postUrlToObject(LIST_REF, restTemplate, GENOMES_FETCH, "{}");
		assertEquals("Checking all results found", 4, result.size());
		assertTrue("ID found", result.get(0).containsKey("id"));
	}
	
	@Test
	public void testSelect() {
		Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, GENOMES_SELECT+"?query=human");
		assertEquals("Checking all results found", 2, Long.parseLong(result.get("resultCount").toString()));
		assertEquals("Checking limited results retrieved", 2, ((List<?>) result.get("results")).size());
		List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
		assertTrue("ID found", results.get(0).containsKey("id"));
	}

}
