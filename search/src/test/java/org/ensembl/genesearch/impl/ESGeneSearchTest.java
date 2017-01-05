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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESGeneSearchTest {

	static Logger log = LoggerFactory.getLogger(ESGeneSearchTest.class);

	static ESTestServer testServer = new ESTestServer();
	static ESSearch search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE,
			DataTypeInfo.fromResource("/genes_datatype_info.json"));

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
		log.info("Reading documents");
		String json = DataUtils.readGzipResource("/nanoarchaeum_equitans_kin4_m.json.gz");
		log.info("Creating test index");
		testServer.indexTestDocs(json, ESSearch.GENE_ESTYPE);
	}

	@Test
	public void fetchAll() {
		log.info("Fetching all genes");
		try {
			search.fetch(Collections.emptyList(), QueryOutput.build(Arrays.asList("_id")));
			fail("Illegal operation succeeded");
		} catch (UnsupportedOperationException e) {
			// OK
		}
	}

	@Test
	public void fetchGenome() {
		log.info("Fetching all genes from genome");
		SearchResult result = search.fetch(
				Arrays.asList(new Query(QueryType.TERM, "genome", "nanoarchaeum_equitans_kin4_m")),
				QueryOutput.build(Arrays.asList("_id")));
		List<Map<String, Object>> ids = result.getResults();
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 598, ids.size());
	}

	@Test
	public void fetchHomologues() {
		String genomeName = "escherichia_coli_str_k_12_substr_mg1655";
		log.info("Fetching homologues to " + genomeName);
		Query genome = new Query(QueryType.TERM, "genome", genomeName);

		SearchResult result = search.fetch(
				Arrays.asList(new Query[] { new Query(QueryType.NESTED, "homologues", genome) }),
				QueryOutput.build(Arrays.asList("_id")));
		List<Map<String, Object>> ids = result.getResults();
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 74, ids.size());
	}

	@Test
	public void fetchTypedOrthologues() {
		String genomeName = "escherichia_coli_str_k_12_substr_mg1655";
		String orthologyType = "ortholog_one2one";
		log.info("Fetching " + orthologyType + " homologues to " + genomeName);
		Query orthology = new Query(QueryType.TERM, "description", orthologyType);
		Query genome = new Query(QueryType.TERM, "genome", genomeName);

		SearchResult result = search.fetch(
				Arrays.asList(new Query[] { new Query(QueryType.NESTED, "homologues", genome, orthology) }),
				QueryOutput.build(Arrays.asList("id")));
		List<Map<String, Object>> ids = result.getResults();
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 53, ids.size());
	}

	@Test
	public void fetchTranslationById() {
		String id = "AAR39271";
		log.info("Fetching genes with translation ID=" + id);

		Query tIdQuery = new Query(QueryType.NESTED, "transcripts",
				new Query(QueryType.NESTED, "translations", new Query(QueryType.TERM, "id", id)));

		SearchResult result = search.fetch(Arrays.asList(new Query[] { tIdQuery }),
				QueryOutput.build(Arrays.asList("id")));
		List<Map<String, Object>> ids = result.getResults();
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 1, ids.size());
	}

	@Test
	public void querySimple() {
		log.info("Querying for all genes");
		QueryResult result = search.query(Collections.emptyList(), QueryOutput.build(Arrays.asList("id")),
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().get(0).containsKey("id"));
		assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
		assertEquals("1 field info only", 1, result.getFields().size());
		assertEquals("ID field only", "id", result.getFields().get(0).getName());
	}

	@Test
	public void queryStartEq() {
		{
			log.info("Querying for start=99803");
			QueryResult result = search.query(Query.build("{\"start\":99803}"),
					QueryOutput.build(Arrays.asList("start")), Collections.emptyList(), 0, 5, Collections.emptyList());
			assertEquals("1 result", 1, result.getResultCount());
			Map<String, Object> gene = result.getResults().get(0);
			assertEquals("Start correct", 99803, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
		}

		{
			log.info("Querying for start=99802");
			QueryResult result = search.query(Query.build("{\"start\":99802}"),
					QueryOutput.build(Arrays.asList("start")), Collections.emptyList(), 0, 5, Collections.emptyList());
			assertEquals("No results", 0, result.getResultCount());
		}
	}

	@Test
	public void queryStartGt() {
		{
			log.info("Querying for start>490882");
			QueryResult result = search.query(Query.build("{\"start\":\">490882\"}"),
					QueryOutput.build(Arrays.asList("start")), Collections.emptyList(), 0, 5, Collections.emptyList());
			assertEquals("1 result", 1, result.getResultCount());
			Map<String, Object> gene = result.getResults().get(0);
			assertEquals("Start correct", 490883, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
		}

		{
			log.info("Querying for start>490883");
			QueryResult result = search.query(Query.build("{\"start\":\">490883\"}"),
					QueryOutput.build(Arrays.asList("start")), Collections.emptyList(), 0, 5, Collections.emptyList());
			assertEquals("No results", 0, result.getResultCount());
		}
		{
			log.info("Querying for start>=490883");
			QueryResult result = search.query(Query.build("{\"start\":\">=490883\"}"),
					QueryOutput.build(Arrays.asList("start")), Collections.emptyList(), 0, 5, Collections.emptyList());
			Map<String, Object> gene = result.getResults().get(0);
			assertEquals("Start correct", 490883, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
		}
	}

	@Test
	public void queryStartLt() {
		{
			log.info("Querying for start<884");
			QueryResult result = search.query(Query.build("{\"start\":\"<884\"}"),
					QueryOutput.build(Arrays.asList("start")), Collections.emptyList(), 0, 5, Collections.emptyList());
			assertEquals("1 result", 1, result.getResultCount());
			Map<String, Object> gene = result.getResults().get(0);
			assertEquals("Start correct", 883, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
		}

		{
			log.info("Querying for start<883");
			QueryResult result = search.query(Query.build("{\"start\":\"<883\"}"),
					QueryOutput.build(Arrays.asList("start")), Collections.emptyList(), 0, 5, Collections.emptyList());
			assertEquals("No results", 0, result.getResultCount());
		}
		{
			log.info("Querying for start<=883");
			QueryResult result = search.query(Query.build("{\"start\":\"<=883\"}"),
					QueryOutput.build(Arrays.asList("start")), Collections.emptyList(), 0, 5, Collections.emptyList());
			Map<String, Object> gene = result.getResults().get(0);
			assertEquals("Start correct", 883, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
		}
	}

	@Test
	public void queryRange() {
		{
			log.info("Querying for start 883-885");
			QueryResult result = search.query(Query.build("{\"start\":\"883-885\"}"),
					QueryOutput.build(Arrays.asList("start", "end")), Collections.emptyList(), 0, 5,
					Collections.emptyList());
			assertEquals("1 result", 1, result.getResultCount());
			Map<String, Object> gene = result.getResults().get(0);
			assertEquals("Start correct", 883, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
		}
		{
			log.info("Querying for start 884-885");
			QueryResult result = search.query(Query.build("{\"start\":\"884-885\"}"),
					QueryOutput.build(Arrays.asList("start", "end")), Collections.emptyList(), 0, 5,
					Collections.emptyList());
			assertEquals("No results", 0, result.getResultCount());
		}
	}

	@Test
	public void queryLocation() {
		log.info("Querying for Chromosome:883-2691");
		QueryResult result = search.query(Query.build("{\"location\":\"Chromosome:883-2691\"}"),
				QueryOutput.build(Arrays.asList("seq_region_name", "start", "end")), Collections.emptyList(), 0, 5,
				Collections.emptyList());
		assertEquals("1 result", 1, result.getResultCount());
		Map<String, Object> gene = result.getResults().get(0);
		assertEquals("Name correct", "Chromosome", String.valueOf(gene.get("seq_region_name")));
		assertEquals("Start correct", 883, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
		assertEquals("End correct", 2691, Integer.valueOf(String.valueOf(gene.get("end"))).intValue());
	}

	@Test
	public void queryLocationStranded() {
		log.info("Querying for Chromosome:883-2691:-1");
		QueryResult result = search.query(Query.build("{\"location\":\"Chromosome:883-2691:-1\"}"),
				QueryOutput.build(Arrays.asList("seq_region_name", "start", "end")), Collections.emptyList(), 0, 5,
				Collections.emptyList());
		assertEquals("0 result", 0, result.getResultCount());
		log.info("Querying for Chromosome:883-2691:1");
		result = search.query(Query.build("{\"location\":\"Chromosome:883-2691:1\"}"),
				QueryOutput.build(Arrays.asList("seq_region_name", "start", "end")), Collections.emptyList(), 0, 5,
				Collections.emptyList());
		assertEquals("1 result", 1, result.getResultCount());
		Map<String, Object> gene = result.getResults().get(0);
		assertEquals("Name correct", "Chromosome", String.valueOf(gene.get("seq_region_name")));
		assertEquals("Start correct", 883, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
		assertEquals("End correct", 2691, Integer.valueOf(String.valueOf(gene.get("end"))).intValue());
	}

	@Test
	public void queryLocations() {
		log.info("Querying for Chromosome:883-2691,Chromosome:2668-3189");
		QueryResult result = search.query(
				Query.build("{\"location\":[\"Chromosome:883-2691\",\"Chromosome:2668-3189\"]}"),
				QueryOutput.build(Arrays.asList("seq_region_name", "start", "end")), Collections.emptyList(), 0, 5,
				Collections.emptyList());
		assertEquals("2 result", 2, result.getResultCount());
		{
			Map<String, Object> gene = result.getResults().get(0);
			assertEquals("Name correct", "Chromosome", String.valueOf(gene.get("seq_region_name")));
			assertEquals("Start correct", 883, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
			assertEquals("End correct", 2691, Integer.valueOf(String.valueOf(gene.get("end"))).intValue());
		}
		{
			Map<String, Object> gene = result.getResults().get(1);
			assertEquals("Name correct", "Chromosome", String.valueOf(gene.get("seq_region_name")));
			assertEquals("Start correct", 2668, Integer.valueOf(String.valueOf(gene.get("start"))).intValue());
			assertEquals("End correct", 3189, Integer.valueOf(String.valueOf(gene.get("end"))).intValue());
		}
	}

	@Test
	public void querySimpleFields() {
		log.info("Querying for all genes");
		QueryResult result = search.query(Collections.emptyList(), QueryOutput.build(Arrays.asList("genome")),
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().get(0).containsKey("id"));
		assertTrue("id found", result.getResults().get(0).containsKey("genome"));
		assertEquals("2 field only", 2, result.getResults().get(0).keySet().size());
		assertEquals("2 field info only", 2, result.getFields().size());
		assertEquals("ID field first", "id", result.getFields().get(0).getName());
		assertEquals("Genome field second", "genome", result.getFields().get(1).getName());
	}

	@Test
	public void queryFacet() {
		log.info("Querying for all genes faceted on genome");
		QueryResult result = search.query(Collections.emptyList(), QueryOutput.build(Arrays.asList("id")),
				Arrays.asList("genome"), 0, 5, Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 1, result.getFacets().size());
		assertTrue("Genome facet", result.getFacets().containsKey("genome"));
		assertEquals("Genome facets", 1, result.getFacets().get("genome").size());
		assertEquals("Genome facet count", 598,
				result.getFacets().get("genome").get("nanoarchaeum_equitans_kin4_m").longValue());
	}

	@Test
	public void querySortAsc() {
		log.info("Querying for all genes sorted by name");
		QueryResult result = search.query(Collections.emptyList(), QueryOutput.build(Arrays.asList("id", "name")),
				Collections.emptyList(), 0, 5, Arrays.asList("+name"));
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertEquals("Name found", "5S_rRNA", result.getResults().get(0).get("name"));
	}

	@Test
	public void querySortDesc() {
		log.info("Querying for all genes reverse sorted by name");
		QueryResult result = search.query(Collections.emptyList(), QueryOutput.build(Arrays.asList("id", "name")),
				Collections.emptyList(), 0, 5, Arrays.asList("-name"));
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		// nulls are always last, so will be the last name, alphabetically
		assertEquals("Name found", "tRNA", result.getResults().get(0).get("name"));
	}

	@Test
	public void querySortNestedAsc() {
		log.info("Querying for all genes sorted by transcripts.start");
		QueryResult result = search.query(Collections.emptyList(),
				QueryOutput.build(Arrays.asList("id", "name", "start", "transcripts.start")), Collections.emptyList(),
				0, 5, Arrays.asList("+transcripts.start"));
		System.out.println(result.getResults());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertEquals("Start found", 883, Integer.parseInt(result.getResults().get(0).get("start").toString()));
	}

	@Test
	public void querySortNestedDesc() {
		log.info("Querying for all genes sorted by -transcripts.start");
		QueryResult result = search.query(Collections.emptyList(),
				QueryOutput.build(Arrays.asList("id", "name", "start", "transcripts.start")), Collections.emptyList(),
				0, 5, Arrays.asList("-transcripts.start"));
		System.out.println(result.getResults());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertEquals("Start found", 490883, Integer.parseInt(result.getResults().get(0).get("start").toString()));
	}

	@Test
	public void querySortNestedFilterAsc() {
		log.info("Querying for all genes sorted by transcripts.start");
		QueryResult result = search.query(Query.build("{\"transcripts\":{\"biotype\":\"protein_coding\"}}"),
				QueryOutput.build(Arrays.asList("id", "name", "start", "transcripts.start")), Collections.emptyList(),
				0, 5, Arrays.asList("+transcripts.start"));
		System.out.println(result.getResults());
		assertEquals("Total hits", 536, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertEquals("Start found", 883, Integer.parseInt(result.getResults().get(0).get("start").toString()));
	}

	@Test
	public void querySource() {
		log.info("Querying for all genes with limit on fields");
		QueryResult result = search.query(Collections.emptyList(),
				QueryOutput.build(Arrays.asList("id", "seq_region_name", "homologues")), Collections.emptyList(), 0, 5,
				Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("Name found", result.getResults().get(0).containsKey("id"));
		assertTrue("Name found", result.getResults().get(0).containsKey("seq_region_name"));
		assertTrue("Name found", result.getResults().get(0).containsKey("homologues"));
	}

	@Test
	public void queryLargeTerms() throws IOException {
		QueryHandler handler = new DefaultQueryHandler();
		String json = DataUtils.readGzipResource("/q08_human_swissprot_full.json.gz");
		List<Query> qs = handler.parseQuery(json);
		search.query(qs, QueryOutput.build(Arrays.asList("id", "name", "homologues")), Collections.emptyList(), 0, 5,
				Collections.emptyList());
	}

	@Test
	public void queryWithOffset() throws IOException {
		log.info("Querying for all genes");
		SearchResult result1 = search.query(Collections.emptyList(), QueryOutput.build(Arrays.asList("id")),
				Collections.emptyList(), 0, 2, Collections.emptyList());
		assertEquals("Got 2 results", 2, result1.getResults().size());

		log.info("Querying for all genes with offset");
		SearchResult result2 = search.query(Collections.emptyList(), QueryOutput.build(Arrays.asList("id")),
				Collections.emptyList(), 1, 2, Collections.emptyList());
		assertEquals("Got 2 results", 2, result2.getResults().size());
		assertTrue("Results 1.1 matches 2.0",
				result1.getResults().get(1).get("id").equals(result2.getResults().get(0).get("id")));
	}

	public void fetchGene() {
		log.info("Fetching a single gene");
		String id = "NEQ043";
		Map<String, Object> gene = search.fetchById(id);
		assertTrue("Gene is not null", gene != null);
		assertEquals("ID correct", id, gene.get("id"));
		assertTrue("Homologues not null", gene.containsKey("homologues"));
		assertTrue("Transcripts not null", gene.containsKey("transcripts"));
	}

	public void fetchGenes() {
		log.info("Fetching list of genes");
		String id1 = "NEQ392";
		String id2 = "NEQ175";
		String id3 = "NEQ225";
		List<Map<String, Object>> genes = search.fetchByIds(id1, id2, id3);
		assertTrue("Genes are not null", genes != null);
		assertEquals("3 genes found", 3, genes.size());
		Set<String> ids = genes.stream().map(gene -> (String) gene.get("id")).collect(Collectors.toSet());
		assertTrue("id1 found", ids.contains(id1));
		assertTrue("id2 found", ids.contains(id2));
		assertTrue("id3 found", ids.contains(id3));
	}

	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
