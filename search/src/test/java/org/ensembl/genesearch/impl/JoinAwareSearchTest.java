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

package org.ensembl.genesearch.impl;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.impl.GeneSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.impl.SearchType;
import org.ensembl.genesearch.test.ESTestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author dstaines
 *
 */
public class JoinAwareSearchTest {

	static Logger log = LoggerFactory.getLogger(ESGeneSearchTest.class);

	static ESTestServer testServer = new ESTestServer();
	static ESSearch search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_TYPE);
	
	// set up a provider
	static SearchRegistry provider = new SearchRegistry().registerSearch(SearchType.GENES, search)
			.registerSearch(SearchType.HOMOLOGUES, search);

	// instantiate a join aware search
	static GeneSearch geneSearch = new GeneSearch(provider);

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
		log.info("Reading documents");
		String json = ESTestServer.readGzipResource("/nanoarchaeum_equitans_kin4_m.json.gz");
		log.info("Creating test index");
		testServer.indexTestDocs(json, ESSearch.GENE_TYPE);
	}

	@Test
	public void queryJoinParalogues() {
		QueryResult results = geneSearch.query(
				Arrays.asList(new Query(QueryType.TERM, "genome", "nanoarchaeum_equitans_kin4_m")),
				Arrays.asList("id", "genome", "description"), Collections.emptyList(), 0, 5, Collections.emptyList(),
				"homologues", Collections.emptyList());
		assertEquals("Checking total count", 47, results.getResultCount());
		assertEquals("Checking returned hits",5, results.getResults().size());
		Map<String,Object> result = results.getResults().get(0);
		assertNotNull("Checking id is returned", result.get("id"));
		assertNotNull("Checking genome is returned", result.get("genome"));
		assertNotNull("Checking description is returned", result.get("description"));
	}

	@Test
	public void fetchJoinParalogues() {
		List<Map<String,Object>> results = geneSearch.fetch(
				Arrays.asList(new Query(QueryType.TERM, "genome", "nanoarchaeum_equitans_kin4_m")),
				Arrays.asList("id", "genome", "description"),
				"homologues", Collections.emptyList());
		assertEquals("Checking total count", 47, results.size());
		Map<String,Object> result = results.get(0);
		assertNotNull("Checking id is returned", result.get("id"));
		assertNotNull("Checking genome is returned", result.get("genome"));
		assertNotNull("Checking description is returned", result.get("description"));
	}
	
	@Test
	public void queryJoinParaloguesFilter() {
		QueryResult results = geneSearch.query(
				Arrays.asList(new Query(QueryType.TERM, "genome", "nanoarchaeum_equitans_kin4_m")),
				Arrays.asList("id", "genome", "description","name"), Collections.emptyList(), 0, 5, Collections.emptyList(),
				"homologues", Arrays.asList(new Query(QueryType.TERM,"GO_expanded","GO:1901576")));
		assertEquals("Checking total count", 12, results.getResultCount());
		assertEquals("Checking returned hits",5, results.getResults().size());
		Map<String,Object> result = results.getResults().get(0);
		assertNotNull("Checking id is returned", result.get("id"));
		assertNotNull("Checking genome is returned", result.get("genome"));
		assertNotNull("Checking description is returned", result.get("description"));
	}

	@Test
	public void fetchJoinParaloguesFilter() {
		List<Map<String,Object>> results = geneSearch.fetch(
				Arrays.asList(new Query(QueryType.TERM, "genome", "nanoarchaeum_equitans_kin4_m")),
				Arrays.asList("id", "genome", "description"),
				"homologues", Arrays.asList(new Query(QueryType.TERM,"GO_expanded","GO:1901576")));
		assertEquals("Checking total count", 12, results.size());
		Map<String,Object> result = results.get(0);
		assertNotNull("Checking id is returned", result.get("id"));
		assertNotNull("Checking genome is returned", result.get("genome"));
		assertNotNull("Checking description is returned", result.get("description"));
	}
	
	@Test
	public void queryJoinTranscripts() {
		QueryResult results = geneSearch.query(
				Arrays.asList(new Query(QueryType.TERM, "genome", "nanoarchaeum_equitans_kin4_m")),
				Arrays.asList("id", "genome", "description"), Collections.emptyList(), 0, 5, Collections.emptyList(),
				"transcripts", Collections.emptyList());
		assertEquals("Checking total count", 598, results.getResultCount());
		assertEquals("Checking returned hits",5, results.getResults().size());
		Map<String,Object> result = results.getResults().get(0);
		assertNotNull("Checking id is returned", result.get("id"));
		assertNotNull("Checking genome is returned", result.get("genome"));
		assertNotNull("Checking description is returned", result.get("description"));
	}

	@Test
	public void fetchJoinTranscripts() {
		SearchResult result = geneSearch.fetch(
				Arrays.asList(new Query(QueryType.TERM, "genome", "nanoarchaeum_equitans_kin4_m")),
				Arrays.asList("id", "genome", "description"),
				"transcripts");
		List<Map<String,Object>> results = result.getResults();
		assertEquals("Checking total count", 598, results.size());
		Map<String,Object> r = results.get(0);
		assertNotNull("Checking id is returned", r.get("id"));
		assertNotNull("Checking genome is returned", r.get("genome"));
		assertNotNull("Checking description is returned", r.get("description"));
	}
	
	@Test
	public void querySimple() {
		log.info("Querying for all genes");
		QueryResult result = geneSearch.query(Collections.emptyList(), Arrays.asList("id"), Collections.emptyList(), 0, 5,
				Collections.emptyList(), null, Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().get(0).containsKey("id"));
		assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
	}
	
	@Test
	public void fetchGenome() {
		log.info("Fetching all genes from genome");
		SearchResult result = geneSearch.fetch(
				Arrays.asList(new Query(QueryType.TERM, "genome", "nanoarchaeum_equitans_kin4_m")),
				Arrays.asList("_id"));
		List<Map<String,Object>> ids = result.getResults();
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 598, ids.size());
	}

	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
