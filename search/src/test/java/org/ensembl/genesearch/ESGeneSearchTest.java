package org.ensembl.genesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.ensembl.genesearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
import org.ensembl.genesearch.test.ESTestServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESGeneSearchTest {

	static Logger log = LoggerFactory.getLogger(ESGeneSearchTest.class);

	static ESTestServer testServer = new ESTestServer();
	static ESGeneSearch search = new ESGeneSearch(testServer.getClient());

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
		log.info("Reading documents");
		String json = ESTestServer.readGzipResource("/nanoarchaeum_equitans_kin4_m.json.gz");
		log.info("Creating test index");
		testServer.createTestIndex(json);
	}

	@Test
	public void fetchAll() {
		log.info("Fetching all genes");
		List<Map<String, Object>> ids = search.fetch(new ArrayList<GeneQuery>(), Arrays.asList("_id"));
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 598, ids.size());
	}

	@Test
	public void fetchHomologues() {
		String genomeName = "escherichia_coli_str_k_12_substr_mg1655";
		log.info("Fetching homologues to " + genomeName);
		GeneQuery genome = new GeneQuery(GeneQueryType.TERM, "genome", genomeName);

		List<Map<String, Object>> ids = search.fetch(
				Arrays.asList(new GeneQuery[] { new GeneQuery(GeneQueryType.NESTED, "homologues", genome) }),
				Arrays.asList("_id"));
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 79, ids.size());
	}

	@Test
	public void fetchTypedOrthologues() {
		String genomeName = "escherichia_coli_str_k_12_substr_mg1655";
		String orthologyType = "ortholog_one2one";
		log.info("Fetching " + orthologyType + " homologues to " + genomeName);
		GeneQuery orthology = new GeneQuery(GeneQueryType.TERM, "description", orthologyType);
		GeneQuery genome = new GeneQuery(GeneQueryType.TERM, "genome", genomeName);

		List<Map<String, Object>> ids = search.fetch(
				Arrays.asList(new GeneQuery[] { new GeneQuery(GeneQueryType.NESTED, "homologues", genome, orthology) }),
				Arrays.asList("id"));
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 63, ids.size());
	}

	@Test
	public void fetchTranslationById() {
		String id = "AAR39271";
		log.info("Fetching genes with translation ID=" + id);

		GeneQuery tIdQuery = new GeneQuery(GeneQueryType.NESTED, "transcripts",
				new GeneQuery(GeneQueryType.NESTED, "translations", new GeneQuery(GeneQueryType.TERM, "id", id)));

		List<Map<String, Object>> ids = search.fetch(Arrays.asList(new GeneQuery[] { tIdQuery }), Arrays.asList("id"));
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 1, ids.size());
	}

	@Test
	public void fetchRange() {
		log.info("Fetching for Chromosome:30000-50000");
		GeneQuery seqRegion = new GeneQuery(GeneQueryType.TERM, "seq_region_name", "Chromosome");
		GeneQuery start = new GeneQuery(GeneQueryType.RANGE, "start", (long) 30000, null);
		GeneQuery end = new GeneQuery(GeneQueryType.RANGE, "end", null, (long) 50000);
		List<Map<String, Object>> results = search.fetch(Arrays.asList(new GeneQuery[] { seqRegion, start, end }),
				Arrays.asList("_id", "seq_region_name", "start", "end"));
		assertEquals("Total hits", 26, results.size());
		for (Map<String, Object> result : results) {
			assertEquals("Chromosome name", "Chromosome", result.get("seq_region_name"));
			assertTrue("Start", (Long.parseLong(String.valueOf(result.get("start")))) >= 30000);
			assertTrue("End", (Long.parseLong(String.valueOf(result.get("end")))) <= 50000);
		}
	}

	@Test
	public void querySimple() {
		log.info("Querying for all genes");
		QueryResult result = search.query(Collections.emptyList(), Arrays.asList("id"), Collections.emptyList(), 5,
				Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().get(0).containsKey("id"));
		assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
	}

	@Test
	public void queryFacet() {
		log.info("Querying for all genes faceted on genome");
		QueryResult result = search.query(Collections.emptyList(), Arrays.asList("id"), Arrays.asList("genome"), 5,
				Collections.emptyList());
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
		QueryResult result = search.query(Collections.emptyList(), Arrays.asList("id", "name"), Collections.emptyList(),
				5, Arrays.asList("+name"));
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertEquals("Name found", "5S_rRNA", result.getResults().get(0).get("name"));
	}

	@Test
	public void querySortDesc() {
		log.info("Querying for all genes reverse sorted by name");
		QueryResult result = search.query(Collections.emptyList(), Arrays.asList("id", "name"), Collections.emptyList(),
				5, Arrays.asList("-name"));
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		// nulls are always last, so will be the last name, alphabetically
		assertEquals("Name found", "tRNA", result.getResults().get(0).get("name"));
	}

	@Test
	public void querySource() {
		log.info("Querying for all genes sorted by name");
		QueryResult result = search.query(Collections.emptyList(), Arrays.asList("id", "name", "homologues"),
				Collections.emptyList(), 5, Collections.emptyList());
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("Name found", result.getResults().get(0).containsKey("id"));
		assertTrue("Name found", result.getResults().get(0).containsKey("name"));
		assertTrue("Name found", result.getResults().get(0).containsKey("homologues"));
	}
	
	@Test
	public void queryLargeTerms() throws IOException {
		QueryHandler handler = new DefaultQueryHandler();
		String json = ESTestServer.readGzipResource("/q08_human_swissprot_full.json.gz");
		List<GeneQuery> qs = handler.parseQuery(json);
		QueryResult result = search.query(qs, Arrays.asList("id", "name", "homologues"),
				Collections.emptyList(), 5, Collections.emptyList());
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
