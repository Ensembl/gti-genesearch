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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.test.MongoTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariantRangeSearchTest {

	static Logger log = LoggerFactory.getLogger(VariantRangeSearchTest.class);

	static ESTestServer testServer = new ESTestServer();
	static DataTypeInfo geneInfo = DataTypeInfo.fromResource("/datatypes/genes_datatype_info.json");
	static ESSearch esGeneSearch = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE,
			geneInfo);

	static MongoTestServer mongoTestServer = new MongoTestServer();
	static DataTypeInfo variantInfo = DataTypeInfo.fromResource("/datatypes/variants_datatype_info.json");
	static Search mSearch = new MongoSearch(mongoTestServer.getCollection(), variantInfo);

	static SearchRegistry provider = new SearchRegistry().registerSearch(SearchType.VARIANTS, mSearch)
			.registerSearch(SearchType.GENES, esGeneSearch);
	static Search search = new VariantSearch(provider);
	static Search geneSearch = new RangeBasedJoinGeneSearch(provider);

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
		log.info("Reading documents");
		String variantJson = DataUtils.readGzipResource("/variants.json.gz");
		mongoTestServer.indexData(variantJson);
		String geneData = DataUtils.readGzipResource("/rice_genes.json.gz");
		testServer.indexTestDocs(geneData, ESSearch.GENES_INDEX,ESSearch.GENE_ESTYPE);
	}


	@Test
	public void testQueryJoinTo() {
		QueryResult result = geneSearch.query(Collections.emptyList(),
				QueryOutput.build("\"id\",{\"variants\":[\"chr\",\"annot\"]}"), Collections.emptyList(), 0, 10,
				Collections.emptyList());
		assertEquals("Checking for correct rows", 2, result.getResults().size());
		Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("EPlOSAG00000002326")).findFirst().get();
		assertTrue("ID found", gene.containsKey("id"));
		assertTrue("Variants found", gene.containsKey("variants"));
		List<Map<String, Object>> variants = (List) gene.get("variants");
		assertEquals("Checking for correct rows", 1, variants.size());
		for (String key : new String[] { "_id", "chr", "annot" }) {
			assertTrue(key + " found", variants.stream().allMatch(v -> v.containsKey(key)));
		}
	}

	@Test
	public void testQueryJoinToCount() {
		QueryResult result = geneSearch.query(Collections.emptyList(),
				QueryOutput.build("\"id\",{\"variants\":[\"count\"]}"), Collections.emptyList(), 0, 10,
				Collections.emptyList());
		System.out.println(result.getResults());
		assertEquals("Checking for correct rows", 2, result.getResults().size());
		Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("EPlOSAG00000002326")).findFirst().get();
		assertTrue("ID found", gene.containsKey("id"));
		assertTrue("Variants found", gene.containsKey("variants"));
		Map<String,Object> variants = (Map<String, Object>) gene.get("variants");
		assertTrue("Count found", variants.containsKey("count"));
		int count = (int)variants.get("count");
		assertEquals("Count correct", 1, count);
	}
	
//	@Test
//	public void testQueryJoinToInner() {
//		QueryResult result = geneSearch.query(QueryHandlerTest.build("{\"variants\":{\"inner\":1}}"),
//				QueryOutput.build("\"id\",{\"variants\":[\"chr\",\"annot\"]}"), Collections.emptyList(), 0, 10,
//				Collections.emptyList());
//		assertEquals("Checking for correct rows", 1, result.getResults().size());
//		Map<String, Object> gene = result.getResults().get(0);
//		assertTrue("ID found", gene.containsKey("id"));
//		assertTrue("Variants found", gene.containsKey("variants"));
//		List<Map<String, Object>> variants = (List) gene.get("variants");
//		assertEquals("Checking for correct rows", 10, variants.size());
//		for (String key : new String[] { "_id", "chr", "annot" }) {
//			assertTrue(key + " found", variants.stream().allMatch(v -> v.containsKey(key)));
//		}
//	}
//	
//	@Test
//	public void testQueryJoinToInnerQuery() {
//		QueryResult result = geneSearch.query(QueryHandlerTest.build("{\"variants\":{\"inner\":1, \"annot\":{\"ct-list\":{\"so\":1631}}}}"),
//				QueryOutput.build("\"id\",{\"variants\":[\"chr\",\"annot\"]}"), Collections.emptyList(), 0, 10,
//				Collections.emptyList());
//		assertEquals("Checking for correct rows", 1, result.getResults().size());
//		Map<String, Object> gene = result.getResults().get(0);
//		assertTrue("ID found", gene.containsKey("id"));
//		assertTrue("Variants found", gene.containsKey("variants"));
//		List<Map<String, Object>> variants = (List) gene.get("variants");
//		assertEquals("Checking for correct rows", 10, variants.size());
//		for (String key : new String[] { "_id", "chr", "annot" }) {
//			assertTrue(key + " found", variants.stream().allMatch(v -> v.containsKey(key)));
//		}
//	}
//
//	@Test
//	public void testQueryJoinToInnerQueryExcl() {
//		QueryResult result = geneSearch.query(QueryHandlerTest.build("{\"variants\":{\"inner\":1, \"annot\":{\"ct-list\":{\"so\":16310}}}}"),
//				QueryOutput.build("\"id\",{\"variants\":[\"chr\",\"annot\"]}"), Collections.emptyList(), 0, 10,
//				Collections.emptyList());
//		assertEquals("Checking for correct rows", 0, result.getResults().size());
//	}
	
	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		mongoTestServer.disconnect();
	}

}
