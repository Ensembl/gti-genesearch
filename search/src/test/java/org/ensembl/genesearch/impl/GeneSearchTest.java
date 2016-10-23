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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
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
public class GeneSearchTest {

	static Logger log = LoggerFactory.getLogger(ESGeneSearchTest.class);

	static ESTestServer testServer = new ESTestServer();
	static ESSearch search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
	static ESSearch gSearch = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENOME_ESTYPE);

	// set up a provider
	static SearchRegistry provider = new SearchRegistry().registerSearch(SearchType.GENES, search)
			.registerSearch(SearchType.GENOMES, gSearch);

	// instantiate a join aware search
	static GeneSearch geneSearch = new GeneSearch(provider);

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
		log.info("Reading documents");
		String json = ESTestServer.readGzipResource("/nanoarchaeum_equitans_kin4_m.json.gz");
		String gJson = ESTestServer.readGzipResource("/genomes.json.gz");
		log.info("Creating test indices");
		testServer.indexTestDocs(json, ESSearch.GENE_ESTYPE);
		testServer.indexTestDocs(gJson, ESSearch.GENOME_ESTYPE);
	}

	@Test
	public void querySimple() {
		log.info("Querying for all genes");
		QueryResult result = geneSearch.query(Collections.emptyList(), QueryOutput.build(Arrays.asList("id")),
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().get(0).containsKey("id"));
		assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
	}
	
	@Test
	public void queryJoin() {
		log.info("Querying for all genes joining to genomes");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
		QueryResult result = geneSearch.query(Collections.emptyList(), o,
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
		assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
		Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes")).findFirst();
		assertTrue("genomes found", genome.isPresent());
		Map<String,Object> genomes = (Map)genome.get().get("genomes");
		assertTrue("genomes.id found", genomes.containsKey("id"));
		assertTrue("genomes.name found", genomes.containsKey("name"));
		assertTrue("genomes.division found", genomes.containsKey("division"));
	}

	@Test
	public void queryJoinQueryFrom() {
		log.info("Querying for all genes joining to genomes");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
		List<Query> q = Query.build("{\"id\":\"NEQ043\"}");
		QueryResult result = geneSearch.query(q, o,
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Total hits", 1, result.getResultCount());
		assertEquals("Fetched hits", 1, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
		assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
		Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes")).findFirst();
		assertTrue("genomes found", genome.isPresent());
		Map<String,Object> genomes = (Map)genome.get().get("genomes");
		assertTrue("genomes.id found", genomes.containsKey("id"));
		assertTrue("genomes.name found", genomes.containsKey("name"));
		assertTrue("genomes.division found", genomes.containsKey("division"));
	}
	
	@Test
	public void queryJoinQueryTo() {
		log.info("Querying for all genes joining to genomes");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
		List<Query> q = Query.build("{\"id\":\"NEQ043\",\"genomes\":{\"division\":\"EnsemblBacteria\"}}");
		QueryResult result = geneSearch.query(q, o,
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Total hits", 1, result.getResultCount());
		assertEquals("Fetched hits", 1, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
		assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
		Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes")).findFirst();
		assertTrue("genomes found", genome.isPresent());
		Map<String,Object> genomes = (Map)genome.get().get("genomes");
		assertTrue("genomes.id found", genomes.containsKey("id"));
		assertTrue("genomes.name found", genomes.containsKey("name"));
		assertTrue("genomes.division found", genomes.containsKey("division"));
	}
	
	@Test
	public void queryJoinQueryToNone() {
		log.info("Querying for all genes joining to genomes");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
		List<Query> q = Query.build("{\"id\":\"NEQ043\",\"genomes\":{\"division\":\"EnsemblFruit\"}}");
		QueryResult result = geneSearch.query(q, o,
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Total hits", 1, result.getResultCount());
		assertEquals("Fetched hits", 1, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
		assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
		Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes")).findFirst();
		assertFalse("genomes found", genome.isPresent());
	}
	
	@Test
	public void fetchSimple() {
		log.info("Fetching for all genes");
		SearchResult result = geneSearch.fetch(Query.build("{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}"), QueryOutput.build(Arrays.asList("id")));
		assertEquals("Total hits", 598, result.getResults().size());
		assertTrue("id found", result.getResults().get(0).containsKey("id"));
		assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
	}
	
	@Test
	public void fetchJoin() {
		log.info("Querying for all genes joining to genomes");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
		SearchResult result = geneSearch.fetch(Query.build("{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}"), o);
		assertEquals("Fetched hits", 598, result.getResults().size());
		assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
		assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
		Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes")).findFirst();
		assertTrue("genomes found", genome.isPresent());
		Map<String,Object> genomes = (Map)genome.get().get("genomes");
		assertTrue("genomes.id found", genomes.containsKey("id"));
		assertTrue("genomes.name found", genomes.containsKey("name"));
		assertTrue("genomes.division found", genomes.containsKey("division"));
	}

	@Test
	public void fetchJoinQueryFrom() {
		log.info("Querying for all genes joining to genomes");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
		List<Query> q = Query.build("{\"id\":\"NEQ043\"}");
		SearchResult result = geneSearch.fetch(q, o);
		assertEquals("Fetched hits", 1, result.getResults().size());
		assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
		assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
		Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes")).findFirst();
		assertTrue("genomes found", genome.isPresent());
		Map<String,Object> genomes = (Map)genome.get().get("genomes");
		assertTrue("genomes.id found", genomes.containsKey("id"));
		assertTrue("genomes.name found", genomes.containsKey("name"));
		assertTrue("genomes.division found", genomes.containsKey("division"));
	}
	
	@Test
	public void fetchJoinQueryTo() {
		log.info("Querying for all genes joining to genomes");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
		List<Query> q = Query.build("{\"id\":\"NEQ043\",\"genomes\":{\"division\":\"EnsemblBacteria\"}}");
		SearchResult result = geneSearch.fetch(q, o);
		assertEquals("Fetched hits", 1, result.getResults().size());
		assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
		assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
		Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes")).findFirst();
		assertTrue("genomes found", genome.isPresent());
		Map<String,Object> genomes = (Map)genome.get().get("genomes");
		assertTrue("genomes.id found", genomes.containsKey("id"));
		assertTrue("genomes.name found", genomes.containsKey("name"));
		assertTrue("genomes.division found", genomes.containsKey("division"));
	}
	
	@Test
	public void fetchJoinQueryToNone() {
		log.info("Querying for all genes joining to genomes");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
		List<Query> q = Query.build("{\"id\":\"NEQ043\",\"genomes\":{\"division\":\"EnsemblFruit\"}}");
		SearchResult result = geneSearch.fetch(q, o);
		assertEquals("Fetched hits", 1, result.getResults().size());
		assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
		assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
		Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes")).findFirst();
		assertFalse("genomes found", genome.isPresent());
	}


	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
