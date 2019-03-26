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

import org.ensembl.genesearch.QueryHandlerTest;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.test.MongoTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link MongoSearch}
 * 
 * @author dstaines
 *
 */
public class MongoSearchTest {

	static Logger log = LoggerFactory.getLogger(MongoSearchTest.class);

	static MongoTestServer testServer;
	static MongoSearch search;

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
        testServer = new MongoTestServer();
        search = new MongoSearch(testServer.getCollection(),
                DataTypeInfo.fromResource("/datatypes/mongo_variants_datatype_info.json"));
		log.info("Reading documents");
		String json = DataUtils.readGzipResource("/variants.json.gz");
		log.info("Creating test index");
		testServer.indexData(json);
	}

	@Test
	public void testQueryAll() {
		QueryResult result = search.query(Collections.emptyList(), QueryOutput.build("[\"chr\",\"annot\"]"),
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Checking for result fields", 3, result.getFields().size());
		assertEquals("Checking for correct rows", 5, result.getResults().size());
	}

	@Test
	public void testQuery() {
		QueryResult result = search.query(QueryHandlerTest.build("{\"chr\":\"Chr1\"}"), QueryOutput.build("[\"chr\",\"annot\"]"),
				Collections.emptyList(), 0, 5, Collections.emptyList());
		assertEquals("Checking for result fields", 3, result.getFields().size());
		assertEquals("Checking for correct rows", 5, result.getResults().size());
		assertTrue("", result.getResults().stream().allMatch(r -> r.get("chr").equals("Chr1")));
	}

	@Test
	public void testFetch() {
		SearchResult result = search.fetch(QueryHandlerTest.build("{\"chr\":\"Chr1\"}"), QueryOutput.build("[\"chr\",\"annot\"]"));
		assertEquals("Checking for result fields", 3, result.getFields().size());
		assertEquals("Checking for correct rows", 10, result.getResults().size());
		assertTrue("Check we've only got Chr 1",
				result.getResults().stream().allMatch(r -> r.get("chr").equals("Chr1")));
	}

	@Test
	public void testFetchConsume() {
		AtomicInteger i = new AtomicInteger(0);
		search.fetch(r -> {
			assertEquals("Checking for result fields", 3, r.keySet().size());
			assertEquals("Checking for Chr1", "Chr1", r.get("chr"));
			i.incrementAndGet();
		}, QueryHandlerTest.build("{\"chr\":\"Chr1\"}"), QueryOutput.build("[\"chr\",\"annot\"]"));
		assertEquals("Checking for correct rows", 10, i.get());
	}

	@Test
	public void testQuerySub() {
		QueryResult result = search.query(QueryHandlerTest.build("{\"annot\":{\"ct-list\":{\"ensg\":\"OS01G0100100\"}}}"),
				QueryOutput.build("[\"chr\",\"annot\"]"), Collections.emptyList(), 0, 5, Collections.emptyList());
		System.out.println(result.getResults());
		assertEquals("Checking for result fields", 3, result.getFields().size());
		assertEquals("Checking for correct rows", 5, result.getResults().size());
		assertTrue("", result.getResults().stream().allMatch(r -> r.get("chr").equals("Chr1")));
	}

	@Test
	public void testQuerySubDub() {
		QueryResult result = search.query(
				QueryHandlerTest.build("{\"annot\":{\"ct-list\":{\"ensg\":\"OS01G0100100\",\"so\":1631}}}"),
				QueryOutput.build("[\"chr\",\"annot\"]"), Collections.emptyList(), 0, 5, Collections.emptyList());
		System.out.println(result.getResults());
		assertEquals("Checking for result fields", 3, result.getFields().size());
		assertEquals("Checking for correct rows", 5, result.getResults().size());
		assertTrue("", result.getResults().stream().allMatch(r -> r.get("chr").equals("Chr1")));
	}

	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}
}
