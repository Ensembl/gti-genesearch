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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.Search;
import org.junit.Test;

/**
 * Basic tests for REST-based sequence retrieval mechanism
 * 
 * @author dstaines
 *
 */
public class DivisionAwareSequenceSearchTest {

	private final static DivisionAwareSequenceSearch search = new DivisionAwareSequenceSearch(
			null,
			new EnsemblRestSequenceSearch("http://rest.ensembl.org/sequence/id"),
			new EnsemblRestSequenceSearch("http://rest.ensemblgenomes.org/sequence/id"));

	public List<Query> buildQuery(List<String> ids, Query... qs) {
		search.isEnsembl = new HashSet<>();
		search.isEnsembl.add("homo_sapiens");
		List<Query> subQ = new ArrayList<>();
		subQ.add(new Query(QueryType.TERM, "id", ids));
		subQ.addAll(Arrays.asList(qs));
		return Arrays.asList(new Query(QueryType.NESTED, "homo_sapiens", subQ.toArray(new Query[]{})));
	}
	
	@Test
	public void testSingleGene() throws IOException {
		String id = getIds("/gene_ids.txt").get(0);
		List<Query> queries = buildQuery(Arrays.asList(id));
		List<Map<String, Object>> seqs = search.fetch(queries, Collections.emptyList());
		assertEquals("Checking single sequence", 1, seqs.size());
		assertEquals("Checking description present", String.valueOf(seqs.get(0).get("id")), id);
		assertTrue("Checking description present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("desc"))));
		assertTrue("Checking sequence present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("seq"))));
	}

	@Test
	public void testMultipleGenes() throws IOException {
		List<String> ids = getIds("/gene_ids.txt");
		List<Query> queries = buildQuery(ids);
		List<Map<String, Object>> seqs = search.fetch(queries, Collections.emptyList());
		assertEquals("Checking correct number of sequences", ids.size(), seqs.size());
		assertTrue("Checking ID present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("id"))));
		assertTrue("Checking description present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("desc"))));
		assertTrue("Checking sequence present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("seq"))));
	}


	@Test
	public void testSingleTranscript() throws IOException {
		String id = getIds("/transcript_ids.txt").get(0);
		List<Query> queries = buildQuery(Arrays.asList(id));
		List<Map<String, Object>> seqs = search.fetch(queries, Collections.emptyList());
		assertEquals("Checking single sequence", 1, seqs.size());
		assertEquals("Checking description present", String.valueOf(seqs.get(0).get("id")), id);
		assertTrue("Checking description present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("desc"))));
		assertTrue("Checking sequence present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("seq"))));
	}

	@Test
	public void testMultipleTranscripts() throws IOException {
		List<String> ids = getIds("/transcript_ids.txt");
		List<Query> queries = buildQuery(ids);
		List<Map<String, Object>> seqs = search.fetch(queries, Collections.emptyList());
		assertEquals("Checking correct number of sequences", ids.size(), seqs.size());
		assertTrue("Checking ID present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("id"))));
		assertTrue("Checking description present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("desc"))));
		assertTrue("Checking sequence present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("seq"))));
	}

	@Test
	public void testSingleProtein() throws IOException {
		String id = getIds("/protein_ids.txt").get(0);
		List<Query> queries = buildQuery(Arrays.asList(id));
		List<Map<String, Object>> seqs = search.fetch(queries, Collections.emptyList());
		assertEquals("Checking single sequence", 1, seqs.size());
		assertEquals("Checking description present", String.valueOf(seqs.get(0).get("id")), id);
		assertTrue("Checking description present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("desc"))));
		assertTrue("Checking sequence present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("seq"))));
	}

	@Test
	public void testMultipleProteins() throws IOException {
		List<String> ids = getIds("/protein_ids.txt");
		List<Query> queries = buildQuery(ids);
		List<Map<String, Object>> seqs = search.fetch(queries, Collections.emptyList());
		assertEquals("Checking correct number of sequences", ids.size(), seqs.size());
		assertTrue("Checking ID present", !StringUtils.isEmpty((String) seqs.get(0).get("id")));
		assertNull("Checking description present", seqs.get(0).get("desc"));
		assertTrue("Checking sequence present", !StringUtils.isEmpty((String) seqs.get(0).get("seq")));
	}

	private List<String> getIds(String name) throws IOException {
		return IOUtils.readLines(this.getClass().getResourceAsStream(name));
	}

	@Test
	public void testChangeType() throws IOException {
		String id = "ENSG00000139618";
		List<Query> queries = buildQuery(Arrays.asList(id),new Query(QueryType.TERM, "type", "protein"));
		List<Map<String, Object>> seqs = search.fetch(queries, Collections.emptyList());
		assertTrue("Checking more than one sequence", seqs.size() > 1);
		assertTrue("Checking ID is protein", String.valueOf(seqs.get(0).get("id")).startsWith("ENSP"));
		assertTrue("Checking description not present", seqs.get(0).get("desc") == null);
		assertTrue("Checking sequence present", !StringUtils.isEmpty(String.valueOf(seqs.get(0).get("seq"))));
	}

	@Test
	public void testExpands() throws IOException {
		String id = "ENSG00000139618";
		List<Query> queries = buildQuery(Arrays.asList(id));
		List<Map<String, Object>> seqs = search.fetch(queries, Collections.emptyList());
		String origSeq = seqs.get(0).get("seq").toString();
		assertFalse("Original sequence found", StringUtils.isEmpty(origSeq));
		
		queries = buildQuery(Arrays.asList(id), new Query(QueryType.TERM,"expand_5prime", "100"),new Query(QueryType.TERM,"expand_3prime", "100"));
		
		seqs = search.fetch(queries,
				Collections.emptyList());
		String newSeq = seqs.get(0).get("seq").toString();
		assertFalse("New sequence found", StringUtils.isEmpty(newSeq));
		assertTrue("Old in new", newSeq.contains(origSeq));
		assertEquals("Checking for correct lengths", origSeq.length() + 200, newSeq.length());
		assertEquals("Checking for position of original sequence", 100, newSeq.lastIndexOf(origSeq));
	}

}
















