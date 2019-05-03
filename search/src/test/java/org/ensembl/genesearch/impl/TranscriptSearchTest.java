/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ensembl.genesearch.impl;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.ensembl.genesearch.*;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class TranscriptSearchTest {

	static Logger log = LoggerFactory.getLogger(TranscriptSearchTest.class);

	static ESTestServer testServer;
	static DataTypeInfo geneInfo = DataTypeInfo.fromResource("/datatypes/genes_datatype_info.json");
	static DataTypeInfo transcriptsInfo = DataTypeInfo.fromResource("/datatypes/transcripts_datatype_info.json");
	static ESSearchFlatten search;
	static ESSearch gSearch;

	// set up a provider
	static SearchRegistry provider = new SearchRegistry().registerSearch(SearchType.TRANSCRIPTS, search)
			.registerSearch(SearchType.GENES, gSearch);

	@BeforeClass
	public static void setUp() throws IOException {
		testServer = new ESTestServer();
		search = new ESSearchFlatten(testServer.getClient(), ESSearch.GENES_INDEX,
				ESSearch.GENE_ESTYPE, "transcripts", "genes", transcriptsInfo);
		gSearch = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE, geneInfo);
		// index a sample of JSON
		log.info("Creating test index");
		testServer.indexTestDocs(DataUtils.readGzipResource("/nanoarchaeum_equitans_kin4_m_genes.json.gz"), ESSearch.GENES_INDEX,ESSearch.GENE_ESTYPE);
        testServer.indexTestDocs(DataUtils.readGzipResource("/nanoarchaeum_equitans_kin4_m_genome.json.gz"), ESSearch.GENOMES_INDEX,ESSearch.GENOME_ESTYPE);
	}

	@Test
	public void queryGenomeFlatten() {
		log.info("Fetching all genes from genome and flattening to transcript");
		QueryOutput o = QueryOutput
				.build("[\"biotype\",\"xrefs\",\"id\",{\"genes\":[\"name\",\"description\",\"genome\"]}]");
		List<Query> q = QueryHandlerTest
				.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		QueryResult result = search.query(q, o, Collections.emptyList(), 0, 10, Collections.emptyList());
		List<Map<String, Object>> transcripts = result.getResults();
		log.info("Fetched " + transcripts.size() + " transcripts");
		transcripts.stream().allMatch(r -> r.get("genes.genome").equals("nanoarchaeum_equitans_kin4_m"));
		transcripts.stream().allMatch(r -> r.get("biotype").equals("protein_coding"));
		transcripts.stream().allMatch(r -> r.containsKey("id"));
		transcripts.stream().allMatch(r -> r.containsKey("xrefs"));
		transcripts.stream().allMatch(r -> r.containsKey("genes.id"));
		assertEquals("Number of transcripts", 10, transcripts.size());
	}

	@Test
	public void fetchGenomeFlatten() {
		log.info("Fetching all genes from genome and flattening to transcript");
		QueryOutput o = QueryOutput
				.build("[\"biotype\",\"id\",\"xrefs\",{\"genes\":[\"name\",\"description\",\"genome\"]}]");
		List<Query> q = QueryHandlerTest
				.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		SearchResult result = search.fetch(q, o);
		List<Map<String, Object>> transcripts = result.getResults();
		System.out.println(transcripts.get(0));
		log.info("Fetched " + transcripts.size() + " transcripts");
		transcripts.stream().allMatch(r -> r.get("genes.genome").equals("nanoarchaeum_equitans_kin4_m"));
		transcripts.stream().allMatch(r -> r.get("biotype").equals("protein_coding"));
		transcripts.stream().allMatch(r -> r.containsKey("id"));
		transcripts.stream().allMatch(r -> r.containsKey("genes.id"));
		transcripts.stream().allMatch(r -> r.containsKey("xrefs"));
		assertEquals("Number of transcripts", 536, transcripts.size());
	}

	@Test
	public void queryGenomeFacetTop() {
		log.info("Fetching all genes from genome and flattening to transcript, faceting by genome");
		QueryOutput o = QueryOutput
				.build("[\"biotype\",\"xrefs\",\"id\",{\"genes\":[\"name\",\"description\",\"genome\"]}]");
		List<Query> q = QueryHandlerTest
				.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		List<String> f = Arrays.asList("genes.genome");
		QueryResult result = search.query(q, o, f, 0, 10, Collections.emptyList());
		List<Map<String, Object>> transcripts = result.getResults();
		log.info("Fetched " + transcripts.size() + " transcripts");
		Map<String, Map<String, Long>> facets = result.getFacets();
		log.info("Facets: " + facets);
		assertTrue("genes.genome facet", facets.containsKey("genes.genome"));
		assertEquals("genes.genome facet set", Long.valueOf(536),
				facets.get("genes.genome").get("nanoarchaeum_equitans_kin4_m"));
	}

	@Test
	public void queryGenomeFacetTranscript() {
		log.info("Fetching all genes from genome and flattening to transcript, faceting by genome");
		QueryOutput o = QueryOutput
				.build("[\"biotype\",\"xrefs\",\"id\",{\"genes\":[\"name\",\"description\",\"genome\"]}]");
		List<Query> q = QueryHandlerTest
				.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		List<String> f = Arrays.asList("biotype");
		QueryResult result = search.query(q, o, f, 0, 10, Collections.emptyList());
		List<Map<String, Object>> transcripts = result.getResults();
		log.info("Fetched " + transcripts.size() + " transcripts");
		Map<String, Map<String, Long>> facets = result.getFacets();
		log.info("Facets: " + facets);
		assertTrue("biotype facet", facets.containsKey("biotype"));
		assertEquals("biotype facet set", Long.valueOf(536), facets.get("biotype").get("protein_coding"));
	}

	@Test
	public void queryGenomeSort() {
		log.info("Fetching all genes from genome and flattening to transcript, sort by seq_region_start");
		QueryOutput o = QueryOutput
				.build("[\"biotype\",\"start\",\"id\",{\"genes\":[\"name\",\"description\",\"genome\",\"start\"]}]");
		List<Query> q = QueryHandlerTest
				.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		QueryResult result = search.query(q, o, Collections.emptyList(), 0, 10, Arrays.asList("-start"));
		List<Map<String, Object>> transcripts = result.getResults();
		log.info("Fetched " + transcripts.size() + " transcripts");
		long last = Long.MAX_VALUE;
		int n = 1;
		for (Map<String, Object> t : transcripts) {
			System.out.println(t);
			long current = Long.parseLong(t.get("start").toString());
			assertTrue("Checking start of transcript " + (n++) + " " + last + " vs " + current, last > current);
			last = current;
		}
	}	

	@AfterClass
	public static void tearDown() throws IOException {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
