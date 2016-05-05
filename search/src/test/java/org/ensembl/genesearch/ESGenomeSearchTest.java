/*
http://gti-es-0.ebi.ac.uk:9200/genomes/genome/_search?pretty&q=K12 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
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

package org.ensembl.genesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.test.ESTestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESGenomeSearchTest {

	static Logger log = LoggerFactory.getLogger(ESGenomeSearchTest.class);

	static ESTestServer testServer = new ESTestServer();
	static ESSearch search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENOME_TYPE);

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
		log.info("Reading documents");
		String json = ESTestServer.readGzipResource("/genomes.json.gz");
		log.info("Creating test index");
		testServer.indexTestDocs(json, ESSearch.GENOME_TYPE);
	}

	@Test
	public void fetchAll() {
		log.info("Fetching all genomes");
		try {
			search.fetch(Collections.emptyList(), Arrays.asList("_id"));
			fail("Illegal operation succeeded");
		} catch (UnsupportedOperationException e) {
			// OK
		}
	}

	@Test
	public void fetchGenomeById() {
		log.info("Fetching  genomes from genome");
		List<Map<String, Object>> ids = search.fetch(Arrays.asList(new Query(QueryType.TERM, "id", "homo_sapiens")),
				Arrays.asList("_id"));
		log.info("Fetched " + ids.size() + " genomes");
		assertEquals("Number of genomes", 1, ids.size());
	}

	@Test
	public void querySimple() {
		log.info("Querying for all genomes");
		QueryResult result = search.query(Collections.emptyList(), Arrays.asList("id"), Collections.emptyList(), 0, 5,
				Collections.emptyList());
		assertEquals("Total hits", 4, result.getResultCount());
		assertEquals("Fetched hits", 4, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().get(0).containsKey("id"));
		assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
	}

	@Test
	public void fetchGenome() {
		log.info("Fetching a single genome");
		String id = "homo_sapiens";
		Map<String, Object> genome = search.fetchById(id);
		assertTrue("Genome is not null", genome != null);
		assertEquals("ID correct", id, genome.get("id"));
	}

	@Test
	public void fetchGenomes() {
		log.info("Fetching list of genomes");
		String id1 = "homo_sapiens";
		String id2 = "escherichia_coli_str_k_12_substr_mg1655";
		List<Map<String, Object>> genomes = search.fetchByIds(id1, id2);
		assertTrue("genomes are not null", genomes != null);
		assertEquals("2 genomes found", 2, genomes.size());
		Set<String> ids = genomes.stream().map(gene -> (String) gene.get("id")).collect(Collectors.toSet());
		assertTrue("id1 found", ids.contains(id1));
		assertTrue("id2 found", ids.contains(id2));
	}
	
	@Test
	public void testSelectHuman() {
		QueryResult results = search.select("human", 0, 10);
		assertTrue("Results found",results!=null);
		assertEquals("2 results",2,results.getResultCount());
		assertEquals("Human first","homo_sapiens",results.getResults().get(0).get("id"));
	}
	
	@Test
	public void testSelectEcoli() {
		QueryResult results = search.select("escherichia", 0, 10);
		assertTrue("Results found",results!=null);
		assertEquals("2 results",2,results.getResultCount());
		assertEquals("K12 first","escherichia_coli_str_k_12_substr_mg1655",results.getResults().get(0).get("id"));
	}


	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
