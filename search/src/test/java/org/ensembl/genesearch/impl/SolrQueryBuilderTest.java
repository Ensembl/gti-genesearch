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

import java.util.Arrays;

import org.apache.solr.client.solrj.SolrQuery;
import org.ensembl.genesearch.Query;
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
		SolrQuery q = SolrQueryBuilder.build(Query.build("{\"A\":\"1\",\"B\":\"2\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Checking " + qStr + " has correct syntax", "A:1 AND B:2", qStr);
	}

	/**
	 * Test multiple value query
	 */
	@Test
	public void testIn() {
		SolrQuery q = SolrQueryBuilder.build(Query.build("{\"A\":[\"1\",\"2\",\"3\"]}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Checking " + qStr + " has correct IN syntax", "A:(1 OR 2 OR 3)", qStr);
	}

	/**
	 * Test compound of multiple value and simple key-value query
	 */
	@Test
	public void testCompound() {
		SolrQuery q = SolrQueryBuilder.build(Query.build("{\"A\":[\"1\",\"2\",\"3\"], \"B\":\"99\"}"));
		String qStr = q.get(SolrQueryBuilder.QUERY_PARAM);
		assertEquals("Checking " + qStr + " has correct IN syntax", "A:(1 OR 2 OR 3) AND B:99", qStr);
	}

	/**
	 * Test construction of a default sort string
	 */
	public void testSimpleSort() {
		String sort = SolrQueryBuilder.parseSort("start");
		assertEquals("Checking " + sort, "start asc", sort);
	}

	/**
	 * Test +clause
	 */
	public void testAscSort() {
		String sort = SolrQueryBuilder.parseSort("+start");
		assertEquals("Checking " + sort, "start asc", sort);
	}

	/**
	 * Test -clause
	 */
	public void testDescSort() {
		String sort = SolrQueryBuilder.parseSort("-start");
		assertEquals("Checking " + sort, "start desc", sort);
	}
	
	/**
	 * Test compound
	 */
	public void testCompoundSort() {
		String sort = SolrQueryBuilder.parseSorts(Arrays.asList(new String[]{"start","-end","+name"}));
		assertEquals("Checking " + sort, "start asc,end desc,name asc", sort);
	}

}
