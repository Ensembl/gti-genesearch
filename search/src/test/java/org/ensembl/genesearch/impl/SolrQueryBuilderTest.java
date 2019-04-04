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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.apache.solr.client.solrj.SolrQuery;
import org.ensembl.genesearch.QueryHandlerTest;
import org.junit.Test;

/**
 * @author dstaines
 *
 */
public class SolrQueryBuilderTest {

	/**
	 * Test simple key-value query
	 */
	@Test
	public void testSimple() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":\"X\",\"B\":\"Y\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Checking " + qStr + " has correct syntax", "A:X AND B:Y", qStr);
	}

	/**
	 * Test multiple value query
	 */
	@Test
	public void testIn() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":[\"X\",\"Y\",\"Z\"]}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Checking " + qStr + " has correct IN syntax", "A:(X OR Y OR Z)", qStr);
	}

	/**
	 * Test compound of multiple value and simple key-value query
	 */
	@Test
	public void testCompound() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":[\"X\",\"Y\",\"Z\"], \"B\":\"BANANA\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Checking " + qStr + " has correct IN syntax", "A:(X OR Y OR Z) AND B:BANANA", qStr);
	}

	/**
	 * Test construction of a default sort string
	 */
	@Test
	public void testSimpleSort() {
		String sort = SolrQueryBuilder.parseSort("start");
		assertEquals("Checking " + sort, "start asc", sort);
	}

	/**
	 * Test +clause
	 */
	@Test
	public void testAscSort() {
		String sort = SolrQueryBuilder.parseSort("+start");
		assertEquals("Checking " + sort, "start asc", sort);
	}

	/**
	 * Test -clause
	 */
	@Test
	public void testDescSort() {
		String sort = SolrQueryBuilder.parseSort("-start");
		assertEquals("Checking " + sort, "start desc", sort);
	}
	
	/**
	 * Test compound
	 */
	@Test
	public void testCompoundSort() {
		String sort = SolrQueryBuilder.parseSorts(Arrays.asList(new String[]{"start","-end","+name"}));
		assertEquals("Checking " + sort, "start asc,end desc,name asc", sort);
	}
	
	/**
	 * Test =
	 */
	@Test
	public void testEq() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":\"0.5\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Eq syntax correct",qStr,"A:0.5");

	}

	/**
	 * Test >
	 */
	@Test
	public void testGt() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":\">0.5\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Gt syntax correct",qStr,"A:{0.5 TO *}");
	}
	
	/**
	 * Test >=
	 */
	@Test
	public void testGte() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":\">=0.5\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Gt syntax correct",qStr,"A:[0.5 TO *]");
	}
	/**
	 * Test <
	 */
	@Test
	public void testLt() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":\"<0.5\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Lt syntax correct",qStr,"A:{* TO 0.5}");
	}
	
	/**
	 * Test <=
	 */
	@Test
	public void testLte() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":\"<=0.5\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Lt syntax correct",qStr,"A:[* TO 0.5]");
	}
	
	/**
	 * Test <=
	 */
	@Test
	public void testRange() {
		SolrQuery q = SolrQueryBuilder.build(QueryHandlerTest.build("{\"A\":\"1-10\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Range syntax correct",qStr,"A:[1 TO 10]");
	}

}
