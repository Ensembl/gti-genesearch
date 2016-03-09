package org.ensembl.genesearch;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.GeneSearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.impl.ESGeneSearch;
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

	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
