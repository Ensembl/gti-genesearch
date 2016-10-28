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

package org.ensembl.genesearch.output;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dstaines
 *
 */
public class ResultsRemodellerTest {

	@Test
	public void testCloneSimpleNoExclude() {
		Map<String, Object> input = parseInput("{\"a\":\"1\",\"b\":[{\"c\":\"1\"},{\"c\":\"2\"}]}");
		Map<String, Object> output = ResultsRemodeller.cloneObject(input, Collections.emptyList());
		assertTrue(output.containsKey("a"));
		assertTrue(output.containsKey("b"));
		List<Map<String, Object>> b = (List<Map<String, Object>>) output.get("b");
		assertEquals(2, b.size());
		assertEquals("1", b.get(0).get("c"));
		assertEquals("2", b.get(1).get("c"));
	}
	
	@Test
	public void testCloneSimpleExclude() {
		Map<String, Object> input = parseInput("{\"a\":\"1\",\"b\":[{\"c\":\"1\"},{\"c\":\"2\"}]}");
		Map<String, Object> output = ResultsRemodeller.cloneObject(input, Arrays.asList("b"));
		assertTrue(output.containsKey("a"));
		assertFalse(output.containsKey("b"));
	}
	
	@Test
	public void testCloneSimpleExcludeChild() {
		Map<String, Object> input = parseInput("{\"a\":\"1\",\"b\":[{\"c\":\"1\"},{\"c\":\"2\"}]}");
		Map<String, Object> output = ResultsRemodeller.cloneObject(input, Arrays.asList("c"));
		assertTrue(output.containsKey("a"));
		assertTrue(output.containsKey("b"));
		List<Map<String, Object>> b = (List<Map<String, Object>>) output.get("b");
		assertEquals(2, b.size());
		assertFalse(b.get(0).containsKey("c"));
		assertFalse(b.get(1).containsKey("c"));
	}

	@Test
	public void testSimple() {
		String input = "{\"a\":\"1\",\"b\":[{\"c\":\"1\"},{\"c\":\"2\"}]}";
		List<Map<String, Object>> flatten = ResultsRemodeller.flatten(parseInput(input), "b");
		assertEquals("2 flattened rows", 2, flatten.size());
		assertEquals("1",flatten.get(0).get("a"));
		assertEquals("1",flatten.get(0).get("b.c"));
		assertEquals("1",flatten.get(1).get("a"));
		assertEquals("2",flatten.get(1).get("b.c"));
	}
	
	@Test
	public void testDeep1() {
		String input = "{\"a\":\"1\",\"b\":[{\"c\":\"1\",\"d\":[{\"e\":\"1\"},{\"e\":\"2\"}]},{\"c\":\"2\",\"d\":[{\"e\":\"3\"},{\"e\":\"4\"}]}]}";
		List<Map<String, Object>> flatten = ResultsRemodeller.flatten(parseInput(input), "b");
		assertEquals("2 flattened rows", 2, flatten.size());
		assertEquals("1",flatten.get(0).get("a"));
		assertEquals("1",flatten.get(0).get("b.c"));
		assertEquals(2,((List<?>)flatten.get(0).get("b.d")).size());
		assertEquals("1",flatten.get(1).get("a"));
		assertEquals("2",flatten.get(1).get("b.c"));
		assertEquals(2,((List<?>)flatten.get(1).get("b.d")).size());
	}
	
	@Test
	public void testDeep2() {
		String input = "{\"a\":\"1\",\"b\":[{\"c\":\"1\",\"d\":[{\"e\":\"1\"},{\"e\":\"2\"}]},{\"c\":\"2\",\"d\":[{\"e\":\"3\"},{\"e\":\"4\"}]}]}";
		List<Map<String, Object>> flatten = ResultsRemodeller.flatten(parseInput(input), "b.d");
		assertEquals("2 flattened rows", 4, flatten.size());
		assertEquals("1",flatten.get(0).get("a"));
		assertEquals("1",flatten.get(0).get("b.c"));
		assertEquals("1",flatten.get(0).get("b.d.e"));
		assertEquals("1",flatten.get(1).get("a"));
		assertEquals("1",flatten.get(1).get("b.c"));
		assertEquals("2",flatten.get(1).get("b.d.e"));
		assertEquals("1",flatten.get(2).get("a"));
		assertEquals("2",flatten.get(2).get("b.c"));
		assertEquals("3",flatten.get(2).get("b.d.e"));
		assertEquals("1",flatten.get(3).get("a"));
		assertEquals("2",flatten.get(3).get("b.c"));
		assertEquals("4",flatten.get(3).get("b.d.e"));
	}
	
	@Test
	public void testSimpleTopLevel() {
		String input = "{\"a\":\"1\",\"b\":[{\"c\":\"1\"},{\"c\":\"2\"}]}";
		List<Map<String, Object>> flatten = ResultsRemodeller.flatten(parseInput(input), "b", "top");
		System.out.println(flatten);
		assertEquals("2 flattened rows", 2, flatten.size());
		assertEquals("1",flatten.get(0).get("top.a").toString());
		assertEquals("1",flatten.get(0).get("c"));
		assertEquals("1",flatten.get(1).get("top.a").toString());
		assertEquals("2",flatten.get(1).get("c"));
	}
	

	protected static Map<String, Object> parseInput(String input) {
		try {
			return new ObjectMapper().readValue(input, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			fail("Could not parse input");
			return null;
		}
	}

}
