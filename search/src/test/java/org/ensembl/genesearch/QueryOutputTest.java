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

package org.ensembl.genesearch;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

/**
 * Tests for creation of {@link QueryOutput}
 * @author dstaines
 *
 */
public class QueryOutputTest {

	/**
	 * Test method for {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
	 */
	@Test
	public void testBuildSimple() {
		String testStr = "\"1\",\"2\",\"3\"";
		QueryOutput o = QueryOutput.build(testStr);
		assertEquals("Checking 3 elements returned", 3, o.getFields().size());
		assertEquals("Checking element 1", "1", o.getFields().get(0));
		assertEquals("Checking element 2", "2", o.getFields().get(1));
		assertEquals("Checking element 3", "3", o.getFields().get(2));
		assertTrue("Checking subFields empty", o.getSubFields().isEmpty());
	}

	/**
	 * Test method for {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
	 */
	@Test
	public void testBuildSimpleArray() {
		String testStr = "[\"1\",\"2\",\"3\"]";
		QueryOutput o = QueryOutput.build(testStr);
		assertEquals("Checking 3 elements returned", 3, o.getFields().size());
		assertEquals("Checking element 1", "1", o.getFields().get(0));
		assertEquals("Checking element 2", "2", o.getFields().get(1));
		assertEquals("Checking element 3", "3", o.getFields().get(2));
		assertTrue("Checking subFields empty", o.getSubFields().isEmpty());
	}
	
	/**
	 * Test method for {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
	 */
	@Test
	public void testBuildSingleHash() {
		String testStr = "{\"genes\":[\"1\",\"2\",\"3\"]}";
		QueryOutput o = QueryOutput.build(testStr);
		assertEquals("Checking 0 elements returned", 0, o.getFields().size());
		assertEquals("Checking for a single subfield",1, o.getSubFields().keySet().size());
		assertTrue("Checking for genes",o.getSubFields().keySet().contains("genes"));
		QueryOutput g = o.getSubFields().get("genes");
		assertEquals("Checking for 3 sub elems",3,g.getFields().size());
		assertEquals("Checking element 1", "1", g.getFields().get(0));
		assertEquals("Checking element 2", "2", g.getFields().get(1));
		assertEquals("Checking element 3", "3", g.getFields().get(2));
		assertTrue("Checking subFields empty", g.getSubFields().isEmpty());
	}
	
	/**
	 * Test method for {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
	 */
	@Test
	public void testBuildDoubleHash() {
		String testStr = "{\"genes\":[\"1\",\"2\",\"3\"],\"variations\":[\"A\",\"B\"]}";
		QueryOutput o = QueryOutput.build(testStr);
		assertEquals("Checking 0 elements returned", 0, o.getFields().size());
		assertEquals("Checking for 2 subfields", 2, o.getSubFields().keySet().size());
		assertTrue("Checking for genes",o.getSubFields().keySet().contains("genes"));
		
		QueryOutput g = o.getSubFields().get("genes");
		assertEquals("Checking for 3 sub elems",3,g.getFields().size());
		assertEquals("Checking element 1", "1", g.getFields().get(0));
		assertEquals("Checking element 2", "2", g.getFields().get(1));
		assertEquals("Checking element 3", "3", g.getFields().get(2));
		assertTrue("Checking subFields empty", g.getSubFields().isEmpty());
		
		assertTrue("Checking for variations",o.getSubFields().keySet().contains("variations"));
		QueryOutput v = o.getSubFields().get("variations");
		assertEquals("Checking for 2 sub elems",2,v.getFields().size());
		assertEquals("Checking element 1", "A", v.getFields().get(0));
		assertEquals("Checking element 2", "B", v.getFields().get(1));
		assertTrue("Checking subFields empty", v.getSubFields().isEmpty());
	}
	
	/**
	 * Test method for {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
	 */
	@Test
	public void testBuildFromArray() {
		QueryOutput o = QueryOutput.build("1","2","3");
		assertEquals("Checking 3 elements returned", 3, o.getFields().size());
		assertEquals("Checking element 1", "1", o.getFields().get(0));
		assertEquals("Checking element 2", "2", o.getFields().get(1));
		assertEquals("Checking element 3", "3", o.getFields().get(2));
		assertTrue("Checking subFields empty", o.getSubFields().isEmpty());
	}

	/**
	 * Test method for {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
	 */
	@Test
	public void testBuildFromList() {
		QueryOutput o = QueryOutput.build(Arrays.asList("1","2","3"));
		assertEquals("Checking 3 elements returned", 3, o.getFields().size());
		assertEquals("Checking element 1", "1", o.getFields().get(0));
		assertEquals("Checking element 2", "2", o.getFields().get(1));
		assertEquals("Checking element 3", "3", o.getFields().get(2));
		assertTrue("Checking subFields empty", o.getSubFields().isEmpty());
	}
	
}
