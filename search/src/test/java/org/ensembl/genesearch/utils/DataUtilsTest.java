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

package org.ensembl.genesearch.utils;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.ensembl.genesearch.utils.DataUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DataUtils}
 * 
 * @author dstaines
 *
 */
public class DataUtilsTest {


    @Test
	public void testGetObjsForKeySimple() {
		Map<String, Object> data = jsonToMap("{\"a\":\"one\", \"c\":\"two\"}");
		Map<String, Map<String,Object>> values = getObjsForKey(data, "a");
		assertTrue("one found", values.containsKey("one"));
		Map<String, Object> oneMap = values.get("one");
		assertEquals("Map contains a:one", "one", oneMap.get("a"));
		assertEquals("Map contains c:two", "two", oneMap.get("c"));
	}
	
	@Test
	public void testGetObjsForKeyNested() {
		Map<String, Object> data = jsonToMap("{\"a\":{\"b\":\"one\", \"c\":\"two\"}}");
		Map<String, Map<String,Object>> values = getObjsForKey(data, "a.b");
		System.out.println(values);
		assertTrue("one found", values.containsKey("one"));
		Map<String, Object> oneMap =  values.get("one");
		assertEquals("Map contains a:one", "one", oneMap.get("b"));
		assertEquals("Map contains c:two", "two", oneMap.get("c"));
	}
	
	@Test
	public void testGetObjsForKeyList() {
		Map<String, Object> data = jsonToMap("{\"a\":[{\"b\":\"one\", \"c\":\"two\"},{\"b\":\"three\", \"c\":\"four\"}]}");
		Map<String, Map<String,Object>> values = getObjsForKey(data, "a.b");
		System.out.println(values);
		assertTrue("one found", values.containsKey("one"));
		assertTrue("three found", values.containsKey("three"));
		Map<String, Object> oneMap = values.get("one");
		assertEquals("Map contains a:one", "one", oneMap.get("b"));
		assertEquals("Map contains c:two", "two", oneMap.get("c"));
		oneMap = values.get("three");
		assertEquals("Map contains a:three", "three", oneMap.get("b"));
		assertEquals("Map contains c:four", "four", oneMap.get("c"));
	}
	
	
	@Test
	public void testGetObjValsForKeyList() {
		Map<String, Object> data = jsonToMap("{\"a\":[{\"b\":\"one\", \"c\":\"two\"},{\"b\":\"three\", \"c\":\"four\"}]}");
		Set<String> values = getObjValsForKey(data, "a.b");
		assertTrue("one found", values.contains("one"));
		assertTrue("three found", values.contains("three"));
	}
	
	@Test
	public void testGetObjValsForList() {
		Map<String, Object> data = jsonToMap("{\"a\":[\"one\", \"two\", \"three\", \"four\"]}");
		Set<String> values = getObjValsForKey(data, "a");
		assertTrue("one found", values.contains("one"));
		assertTrue("two found", values.contains("two"));
		assertTrue("three found", values.contains("three"));
		assertTrue("four found", values.contains("four"));
	}
	
}
