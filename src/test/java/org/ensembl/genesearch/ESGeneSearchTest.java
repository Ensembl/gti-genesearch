package org.ensembl.genesearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.ensembl.genesearch.GeneSearch.GeneQuery;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESGeneSearchTest {

	Logger log = LoggerFactory.getLogger(this.getClass());

	ESTestServer testServer = new ESTestServer();
	ESGeneSearch search = new ESGeneSearch(testServer.getClient());

	@Before
	public void setUp() throws IOException {
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
		assertEquals("Number of genes",598,ids.size());
	}

	@After
	public void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
