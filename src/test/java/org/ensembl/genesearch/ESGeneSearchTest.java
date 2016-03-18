package org.ensembl.genesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.ensembl.genesearch.GeneSearch.QuerySort;
import org.ensembl.genesearch.GeneSearch.QuerySort.SortDirection;
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
		String json = ESTestServer
				.readGzipResource("/nanoarchaeum_equitans_kin4_m.json.gz");
		log.info("Creating test index");
		testServer.createTestIndex(json);
	}

	@Test
	public void fetchAll() {
		log.info("Fetching all genes");
		List<Map<String, Object>> ids = search.query(
				new ArrayList<GeneQuery>(), "_id");
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 598, ids.size());
	}

	@Test
	public void fetchHomologues() {
		String genomeName = "escherichia_coli_str_k_12_substr_mg1655";
		log.info("Fetching homologues to " + genomeName);
		GeneQuery genome = new GeneQuery(GeneQueryType.TERM, "genome",
				genomeName);

		List<Map<String, Object>> ids = search.query(Arrays
				.asList(new GeneQuery[] { new GeneQuery(GeneQueryType.NESTED,
						"homologues", genome) }), "_id");
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 79, ids.size());
	}

	@Test
	public void fetchTypedOrthologues() {
		String genomeName = "escherichia_coli_str_k_12_substr_mg1655";
		String orthologyType = "ortholog_one2one";
		log.info("Fetching " + orthologyType + " homologues to " + genomeName);
		GeneQuery orthology = new GeneQuery(GeneQueryType.TERM, "description",
				orthologyType);
		GeneQuery genome = new GeneQuery(GeneQueryType.TERM, "genome",
				genomeName);

		List<Map<String, Object>> ids = search.query(Arrays
				.asList(new GeneQuery[] { new GeneQuery(GeneQueryType.NESTED,
						"homologues", genome, orthology) }), "_id");
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 63, ids.size());
	}

	@Test
	public void fetchTranslationById() {
		String id = "AAR39271";
		log.info("Fetching genes with translation ID=" + id);

		GeneQuery tIdQuery = new GeneQuery(GeneQueryType.NESTED, "transcripts",
				new GeneQuery(GeneQueryType.NESTED, "translations",
						new GeneQuery(GeneQueryType.TERM, "id", id)));

		List<Map<String, Object>> ids = search.query(
				Arrays.asList(new GeneQuery[] { tIdQuery }), "_id");
		log.info("Fetched " + ids.size() + " genes");
		assertEquals("Number of genes", 1, ids.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void querySimple() {
		log.info("Querying for all genes");
		QueryResult result = search.query(Collections.EMPTY_LIST,
				Arrays.asList("id"), Collections.EMPTY_LIST, 5,
				Collections.EMPTY_LIST);
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("id found", result.getResults().get(0).containsKey("id"));
		assertEquals("1 field only", 1, result.getResults().get(0).keySet()
				.size());
	}
	
	@Test
	public void queryFacet() {
		log.info("Querying for all genes faceted on genome");
		QueryResult result = search.query(Collections.EMPTY_LIST,
				Arrays.asList("id"), Arrays.asList("genome"), 5,
				Collections.EMPTY_LIST);
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 1, result.getFacets().size());
		assertTrue("Genome facet", result.getFacets().containsKey("genome"));
		assertEquals("Genome facets", 1, result.getFacets().get("genome").size());
		assertEquals("Genome facet count", 598, result.getFacets().get("genome").get("nanoarchaeum_equitans_kin4_m").longValue());
	}

	@Test
	public void querySort() {
		log.info("Querying for all genes sorted by name");
		QueryResult result = search.query(Collections.EMPTY_LIST,
				Arrays.asList("id","name"), Collections.EMPTY_LIST, 5,
				Arrays.asList(new QuerySort("name",SortDirection.ASC)));
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertEquals("Name found", "5S_rRNA", result.getResults().get(0).get("name"));
	}

	public void querySource() {
		log.info("Querying for all genes sorted by name");
		QueryResult result = search.query(Collections.EMPTY_LIST,
				Arrays.asList("id","name","homologues"), Collections.EMPTY_LIST, 5,
				Collections.EMPTY_LIST);
		assertEquals("Total hits", 598, result.getResultCount());
		assertEquals("Fetched hits", 5, result.getResults().size());
		assertEquals("Total facets", 0, result.getFacets().size());
		assertTrue("Name found", result.getResults().get(0).containsKey("id"));
		assertTrue("Name found", result.getResults().get(0).containsKey("name"));
		assertTrue("Name found", result.getResults().get(0).containsKey("homologues"));
	}

	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
