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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.bson.Document;
import org.ensembl.genesearch.Query;
import org.junit.Test;

/**
 * @author dstaines
 *
 */
public class MongoSearchBuilderTest {

	@Test
	public void testSimple() {
		List<Query> q = Query.build("{\"chr\":\"Chr1\"}");
		Document doc = MongoSearchBuilder.buildQuery(q);
		assertEquals("chr:Chr1 found", "Chr1", doc.get("chr"));
	}

	@Test
	public void testSimpleList() {
		List<Query> q = Query.build("{\"chr\":\"Chr1\",\"fruit\":\"banana\"}");
		Document doc = MongoSearchBuilder.buildQuery(q);
		assertEquals("chr:Chr1 found", "Chr1", doc.get("chr"));
		assertEquals("fruit:banana found", "banana", doc.get("fruit"));
	}

	@Test
	public void testNested() {
		List<Query> q = Query.build("{\"A\":{\"B\":\"C\"}}");
		Document doc = MongoSearchBuilder.buildQuery(q);
		System.out.println(doc);
		Object subDoc = doc.get("A.B");
		assertNotNull("A.B found", subDoc);
		assertEquals("B:C found", "C", subDoc);
	}

	@Test
	public void testNestedArray() {
		List<Query> q = Query.build("{\"A-list\":{\"B\":\"C\"}}");
		Document doc = MongoSearchBuilder.buildQuery(q);
		System.out.println(doc);
		Object subDoc = doc.get("A");
		assertNotNull("A found", subDoc);
		assertTrue("A is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
		Object subDoc2 = ((Document) subDoc).get("$elemMatch");
		assertNotNull("$elemMatch found", subDoc);
		assertTrue("$elemMatch is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
		assertEquals("B:C found", "C", ((Document) subDoc2).get("B"));
	}

	@Test
	public void testNestedArrayDouble() {
		List<Query> q = Query.build("{\"A-list\":{\"B\":\"C\",\"D\":\"E\"}}");
		Document doc = MongoSearchBuilder.buildQuery(q);
		System.out.println(doc);
		Object subDoc = doc.get("A");
		assertNotNull("A found", subDoc);
		assertTrue("A is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
		Object subDoc2 = ((Document) subDoc).get("$elemMatch");
		assertNotNull("$elemMatch found", subDoc);
		assertTrue("$elemMatch is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
		assertEquals("B:C found", "C", ((Document) subDoc2).get("B"));
		assertEquals("D:E found", "E", ((Document) subDoc2).get("D"));
	}

	@Test
	public void testNestedSubArray() {
		List<Query> q = Query.build("{\"top\":{\"A-list\":{\"B\":\"C\"}}}");
		Document doc = MongoSearchBuilder.buildQuery(q);
		System.out.println(doc);
		Object subDoc = doc.get("top.A");
		assertNotNull("top.A found", subDoc);
		assertTrue("top.A is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
		Object subDoc2 = ((Document) subDoc).get("$elemMatch");
		assertNotNull("$elemMatch found", subDoc);
		assertTrue("$elemMatch is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
		assertEquals("B:C found", "C", ((Document) subDoc2).get("B"));
	}

	@Test
	public void testNestedSubArrayDouble() {
		List<Query> q = Query.build("{\"top\":{\"A-list\":{\"B\":\"C\"},\"X-list\":{\"Y\":\"Z\"}}}");
		Document doc = MongoSearchBuilder.buildQuery(q);
		System.out.println(doc);
		{
			Object subDoc = doc.get("top.A");
			assertNotNull("top.A found", subDoc);
			assertTrue("top.A is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
			Object subDoc2 = ((Document) subDoc).get("$elemMatch");
			assertNotNull("$elemMatch found", subDoc);
			assertTrue("$elemMatch is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
			assertEquals("B:C found", "C", ((Document) subDoc2).get("B"));
		}
		{
			Object subDoc = doc.get("top.X");
			assertNotNull("top.X found", subDoc);
			assertTrue("top. is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
			Object subDoc2 = ((Document) subDoc).get("$elemMatch");
			assertNotNull("$elemMatch found", subDoc);
			assertTrue("$elemMatch is a Document", Document.class.isAssignableFrom(subDoc.getClass()));
			assertEquals("Y:Z found", "Z", ((Document) subDoc2).get("Y"));
		}

	}
	
	@Test
	public void testNums() {
		List<Query> q = Query.build("{\"so\":1234}");
		Document doc = MongoSearchBuilder.buildQuery(q);
		assertTrue("so is a number",Number.class.isAssignableFrom(doc.get("so").getClass()));
	}

}
