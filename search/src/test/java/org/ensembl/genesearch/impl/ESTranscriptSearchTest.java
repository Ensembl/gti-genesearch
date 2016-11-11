/*
http://gti-es-0.ebi.ac.uk:9200/genes/genome/_search?pretty&q=K12 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.test.ESTestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESTranscriptSearchTest {

	static Logger log = LoggerFactory.getLogger(ESTranscriptSearchTest.class);

	static ESTestServer testServer = new ESTestServer();
	static ESSearchFlatten search = new ESSearchFlatten(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE,
			"transcripts", "genes", DataTypeInfo.fromResource("/transcripts_datatype_info.json"));

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
		log.info("Reading documents");
		String json = ESTestServer.readGzipResource("/nanoarchaeum_equitans_kin4_m.json.gz");
		log.info("Creating test index");
		testServer.indexTestDocs(json, ESSearch.GENE_ESTYPE);
	}
	
	@Test 
	public void transformOutput() {
		QueryOutput o = QueryOutput.build("[\"biotype\",\"id\",{\"genes\":[\"name\",\"description\"]}]");
		QueryOutput o2 = search.transformOutput(o);
		assertTrue("No subfields",o2.getSubFields().isEmpty());
		assertTrue("Fields contains transcripts.id", o2.getFields().stream().anyMatch(f-> f.equals("transcripts.id")));
		assertTrue("Fields contains transcripts.biotype", o2.getFields().stream().anyMatch(f-> f.equals("transcripts.biotype")));
		assertTrue("Fields contains name", o2.getFields().stream().anyMatch(f-> f.equals("name")));
		assertTrue("Fields contains description", o2.getFields().stream().anyMatch(f-> f.equals("description")));		
		System.out.println(o2);
	}
	
	@Test 
	public void transformQuery() {
		List<Query> qs = Query.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		List<Query> qs2 = search.transformQueries(qs);
		System.out.println(qs2);
		assertTrue("genome at top level", qs2.stream().anyMatch(q -> q.getFieldName().equals("genome")));
		Optional<Query> transcripts =  qs2.stream().filter(q -> q.getFieldName().equals("transcripts")).findFirst();
		assertTrue("transcripts found", transcripts.isPresent());
		assertTrue("transcripts.biotype found", transcripts.get().getSubQueries()[0].getFieldName().equals("biotype"));
	}
	
	@Test 
	public void transformFields() {
		List<String> fields = Arrays.asList("biotype","genes.genome","-start","-genes.end");
		List<String> newFields = search.transformFields(fields);
		assertTrue("transcripts.biotype found", newFields.contains("transcripts.biotype"));
		assertTrue("genome found",newFields.contains("genome"));
		assertTrue("transcripts.biotype found", newFields.contains("-transcripts.start"));
		assertTrue("genome found",newFields.contains("-end"));
		System.out.println(newFields);
	}
	
	@Test
	public void queryGenomeFlatten() {
		log.info("Fetching all genes from genome and flattening to transcript");
		QueryOutput o = QueryOutput.build("[\"biotype\",\"xrefs\",\"id\",{\"genes\":[\"name\",\"description\",\"genome\"]}]");
		List<Query> q = Query.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		QueryResult result = search.query(
				q,
				o,Collections.emptyList(),0,10, Collections.emptyList());
		List<Map<String, Object>> transcripts = result.getResults();
		log.info("Fetched " + transcripts.size() + " transcripts");
		assertEquals("Number of transcripts", 10, transcripts.size());
		assertTrue("Genome present",transcripts.stream().allMatch(r -> r.get("genes.genome").equals("nanoarchaeum_equitans_kin4_m")));
		assertTrue("Biotype present",transcripts.stream().allMatch(r -> r.get("biotype").equals("protein_coding")));
		assertTrue("ID present",transcripts.stream().allMatch(r -> r.containsKey("id")));
		assertTrue("Xrefs present",transcripts.stream().allMatch(r -> r.containsKey("xrefs")));
		assertTrue("Genes ID present",transcripts.stream().allMatch(r -> r.containsKey("genes.id")));	
		List<FieldInfo> fields = result.getFields();
		System.out.println(fields);
		assertEquals("Number of fields", 6, fields.size());
	}

	@Test
	public void fetchGenomeFlatten() {
		log.info("Fetching all genes from genome and flattening to transcript");
		QueryOutput o = QueryOutput.build("[\"biotype\",\"id\",\"xrefs\",{\"genes\":[\"name\",\"description\",\"genome\"]}]");
		List<Query> q = Query.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		SearchResult result = search.fetch(
				q,
				o);
		List<Map<String, Object>> transcripts = result.getResults();
		System.out.println(transcripts.get(0));
		log.info("Fetched " + transcripts.size() + " transcripts");
		assertEquals("Number of transcripts", 536, transcripts.size());
		assertTrue("Genome present",transcripts.stream().allMatch(r -> r.get("genes.genome").equals("nanoarchaeum_equitans_kin4_m")));
		assertTrue("Biotype present",transcripts.stream().allMatch(r -> r.get("biotype").equals("protein_coding")));
		assertTrue("ID present",transcripts.stream().allMatch(r -> r.containsKey("id")));
		assertTrue("Xrefs present",transcripts.stream().allMatch(r -> r.containsKey("xrefs")));
		assertTrue("Genes ID present",transcripts.stream().allMatch(r -> r.containsKey("genes.id")));	
	}
	
	@Test
	public void queryGenomeFacetTop() {
		log.info("Fetching all genes from genome and flattening to transcript, faceting by genome");
		QueryOutput o = QueryOutput.build("[\"biotype\",\"xrefs\",\"id\",{\"genes\":[\"name\",\"description\",\"genome\"]}]");
		List<Query> q = Query.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		List<String> f = Arrays.asList("genes.genome");
		QueryResult result = search.query(
				q,
				o,f,0,10, Collections.emptyList());
		List<Map<String, Object>> transcripts = result.getResults();
		log.info("Fetched " + transcripts.size() + " transcripts");
		Map<String, Map<String, Long>> facets = result.getFacets();
		log.info("Facets: "+facets);
		assertTrue("genes.genome facet", facets.containsKey("genes.genome"));
		assertEquals("genes.genome facet set", Long.valueOf(536), facets.get("genes.genome").get("nanoarchaeum_equitans_kin4_m"));
	}
	
	@Test
	public void queryGenomeFacetTranscript() {
		log.info("Fetching all genes from genome and flattening to transcript, faceting by genome");
		QueryOutput o = QueryOutput.build("[\"biotype\",\"xrefs\",\"id\",{\"genes\":[\"name\",\"description\",\"genome\"]}]");
		List<Query> q = Query.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		List<String> f = Arrays.asList("biotype");
		QueryResult result = search.query(
				q,
				o,f,0,10, Collections.emptyList());
		List<Map<String, Object>> transcripts = result.getResults();
		log.info("Fetched " + transcripts.size() + " transcripts");
		Map<String, Map<String, Long>> facets = result.getFacets();
		log.info("Facets: "+facets);
		assertTrue("biotype facet", facets.containsKey("biotype"));
		assertEquals("biotype facet set", Long.valueOf(536), facets.get("biotype").get("protein_coding"));
	}
	
	@Test
	public void queryGenomeSort() {
		log.info("Fetching all genes from genome and flattening to transcript, sort by seq_region_start");
		QueryOutput o = QueryOutput.build("[\"biotype\",\"start\",\"id\",{\"genes\":[\"name\",\"description\",\"genome\",\"start\"]}]");
		List<Query> q = Query.build("{\"biotype\":\"protein_coding\", \"genes\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}}");
		QueryResult result = search.query(
				q,
				o,Collections.emptyList(),0,10, Arrays.asList("-start"));
		List<Map<String, Object>> transcripts = result.getResults();
		log.info("Fetched " + transcripts.size() + " transcripts");
		long last = Long.MAX_VALUE;
		int n = 1;
		for(Map<String,Object> t: transcripts) {
			System.out.println(t);
			long current = Long.parseLong(t.get("start").toString());
			assertTrue("Checking start of transcript "+(n++)+" "+last+" vs "+current,last>current);
			last = current;
		}
	}

	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
