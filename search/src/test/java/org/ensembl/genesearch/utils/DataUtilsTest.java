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

import static org.ensembl.genesearch.utils.DataUtils.getObjsForKey;
import static org.ensembl.genesearch.utils.DataUtils.jsonToMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.ensembl.genesearch.utils.DataUtils;
import org.junit.Test;

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
		Map<String, Object> values = getObjsForKey(data, "a");
		assertTrue("one found", values.containsKey("one"));
		Object oneObj = values.get("one");
		assertTrue("Map retrieved", Map.class.isAssignableFrom(oneObj.getClass()));
		Map<String, Object> oneMap = (Map<String, Object>) oneObj;
		assertEquals("Map contains a:one", "one", oneMap.get("a"));
		assertEquals("Map contains c:two", "two", oneMap.get("c"));
	}
	
	@Test
	public void testGetObjsForKeyNested() {
		Map<String, Object> data = jsonToMap("{\"a\":{\"b\":\"one\", \"c\":\"two\"}}");
		Map<String, Object> values = getObjsForKey(data, "a.b");
		System.out.println(values);
		assertTrue("one found", values.containsKey("one"));
		Object oneObj = values.get("one");
		assertTrue("Map retrieved", Map.class.isAssignableFrom(oneObj.getClass()));
		Map<String, Object> oneMap = (Map<String, Object>) oneObj;
		assertEquals("Map contains a:one", "one", oneMap.get("b"));
		assertEquals("Map contains c:two", "two", oneMap.get("c"));
	}
	
	@Test
	public void testGetObjsForKeyList() {
		Map<String, Object> data = jsonToMap("{\"a\":[{\"b\":\"one\", \"c\":\"two\"},{\"b\":\"three\", \"c\":\"four\"}]}");
		Map<String, Object> values = getObjsForKey(data, "a.b");
		System.out.println(values);
		assertTrue("one found", values.containsKey("one"));
		assertTrue("three found", values.containsKey("three"));
		Object oneObj = values.get("one");
		assertTrue("Map retrieved", Map.class.isAssignableFrom(oneObj.getClass()));
		Map<String, Object> oneMap = (Map<String, Object>) oneObj;
		assertEquals("Map contains a:one", "one", oneMap.get("b"));
		assertEquals("Map contains c:two", "two", oneMap.get("c"));
		oneObj = values.get("three");
		assertTrue("Map retrieved", Map.class.isAssignableFrom(oneObj.getClass()));
		oneMap = (Map<String, Object>) oneObj;
		assertEquals("Map contains a:three", "three", oneMap.get("b"));
		assertEquals("Map contains c:four", "four", oneMap.get("c"));
	}
	

}
