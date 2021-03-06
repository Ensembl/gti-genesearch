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

package org.ensembl.genesearch.utils;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.ensembl.genesearch.utils.DataUtils.*;
import org.junit.Assert;

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
		Assert.assertTrue("one found", values.containsKey("one"));
		Map<String, Object> oneMap = values.get("one");
		Assert.assertEquals("Map contains a:one", "one", oneMap.get("a"));
		Assert.assertEquals("Map contains c:two", "two", oneMap.get("c"));
	}
	
	@Test
	public void testGetObjsForKeyNested() {
		Map<String, Object> data = jsonToMap("{\"a\":{\"b\":\"one\", \"c\":\"two\"}}");
		Map<String, Map<String,Object>> values = getObjsForKey(data, "a.b");
		//System.out.println(values);
		Assert.assertTrue("one found", values.containsKey("one"));
		Map<String, Object> oneMap =  values.get("one");
		Assert.assertEquals("Map contains a:one", "one", oneMap.get("b"));
		Assert.assertEquals("Map contains c:two", "two", oneMap.get("c"));
	}
	
	@Test
	public void testGetObjsForKeyList() {
		Map<String, Object> data = jsonToMap("{\"a\":[{\"b\":\"one\", \"c\":\"two\"},{\"b\":\"three\", \"c\":\"four\"}]}");
		Map<String, Map<String,Object>> values = getObjsForKey(data, "a.b");
		//System.out.println(values);
		Assert.assertTrue("one found", values.containsKey("one"));
		Assert.assertTrue("three found", values.containsKey("three"));
		Map<String, Object> oneMap = values.get("one");
		Assert.assertEquals("Map contains a:one", "one", oneMap.get("b"));
		Assert.assertEquals("Map contains c:two", "two", oneMap.get("c"));
		oneMap = values.get("three");
		Assert.assertEquals("Map contains a:three", "three", oneMap.get("b"));
		Assert.assertEquals("Map contains c:four", "four", oneMap.get("c"));
	}
	
	
	@Test
	public void testGetObjValsForKeyList() {
		Map<String, Object> data = jsonToMap("{\"a\":[{\"b\":\"one\", \"c\":\"two\"},{\"b\":\"three\", \"c\":\"four\"}]}");
		//System.out.println(data);
		Set<String> values = getObjValsForKey(data, "a.b");
		Assert.assertTrue("one found", values.contains("one"));
		Assert.assertTrue("three found", values.contains("three"));
	}
	
	@Test
	public void testGetObjValsForList() {
		Map<String, Object> data = jsonToMap("{\"a\":[\"one\", \"two\", \"three\", \"four\"]}");
		Set<String> values = getObjValsForKey(data, "a");
		Assert.assertTrue("one found", values.contains("one"));
		Assert.assertTrue("two found", values.contains("two"));
		Assert.assertTrue("three found", values.contains("three"));
		Assert.assertTrue("four found", values.contains("four"));
	}
	
}
