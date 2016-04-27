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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
import org.ensembl.genesearch.test.ESTestServer;
import org.junit.Test;

public class QueryHandlerTest {

	@Test
	public void testSimpleSingle() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> q = handler.parseQuery("{\"key\":\"value\"}");
		assertEquals("Single query", 1, q.size());
		assertEquals("Query type", GeneQueryType.TERM, q.get(0).getType());
		assertEquals("Query field", "key", q.get(0).getFieldName());
		assertEquals("Query value size", 1, q.get(0).getValues().length);
		assertEquals("Query value", "value", q.get(0).getValues()[0]);
	}

	@Test
	public void testSimpleMultiple() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> q = handler.parseQuery("{\"key\":[\"1\",\"2\"]}");
		assertEquals("Single query", 1, q.size());
		assertEquals("Query type", GeneQueryType.TERM, q.get(0).getType());
		assertEquals("Query field", "key", q.get(0).getFieldName());
		assertEquals("Query value size", 2, q.get(0).getValues().length);
		assertEquals("Query value", "1", q.get(0).getValues()[0]);
		assertEquals("Query value", "2", q.get(0).getValues()[1]);
	}

	@Test
	public void testDouble() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> q = handler.parseQuery("{\"key1\":\"1\",\"key2\":\"2\"}");
		System.out.println(q);
		assertEquals("Double query", 2, q.size());
		assertEquals("Query type", GeneQueryType.TERM, q.get(0).getType());
		assertEquals("Query field", "key1", q.get(0).getFieldName());
		assertEquals("Query value size", 1, q.get(0).getValues().length);
		assertEquals("Query value", "1", q.get(0).getValues()[0]);
		assertEquals("Query type", GeneQueryType.TERM, q.get(1).getType());
		assertEquals("Query field", "key2", q.get(1).getFieldName());
		assertEquals("Query value size", 1, q.get(1).getValues().length);
		assertEquals("Query value", "2", q.get(1).getValues()[0]);
	}

	@Test
	public void testNested() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> qs = handler.parseQuery("{\"key1\":{\"a\":\"1\",\"b\":\"2\"}}");
		System.out.println(qs);
		assertEquals("Single query", 1, qs.size());
		GeneQuery q = qs.get(0);
		assertEquals("Query type", GeneQueryType.NESTED, q.getType());
		assertEquals("Query field", "key1", q.getFieldName());
		assertEquals("Subqueries", 2, q.getSubQueries().length);
		GeneQuery subQ1 = q.getSubQueries()[0];
		assertEquals("Query type", GeneQueryType.TERM, subQ1.getType());
		assertEquals("Query field", "a", subQ1.getFieldName());
		assertEquals("Query value size", 1, subQ1.getValues().length);
		assertEquals("Query value", "1", subQ1.getValues()[0]);
		GeneQuery subQ2 = q.getSubQueries()[1];
		assertEquals("Query type", GeneQueryType.TERM, subQ2.getType());
		assertEquals("Query field", "b", subQ2.getFieldName());
		assertEquals("Query value size", 1, subQ2.getValues().length);
		assertEquals("Query value", "2", subQ2.getValues()[0]);
	}

	@Test
	public void testLocation() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> qs = handler
				.parseQuery("{\"location\":{\"seq_region_name\":\"chr1\",\"start\":\"2\",\"end\":\"10\"}}");
		System.out.println(qs);
		assertEquals("Multi query", 3, qs.size());
		assertEquals("seq_region type", GeneQueryType.TERM, qs.get(0).getType());
		assertEquals("seq_region name", "chr1", qs.get(0).getValues()[0]);
		assertEquals("start name", GeneQueryType.RANGE, qs.get(1).getType());
		assertEquals("start type", new Long(2), qs.get(1).getStart());
		assertEquals("end name", GeneQueryType.RANGE, qs.get(2).getType());
		assertEquals("end type", new Long(10), qs.get(2).getEnd());
	}

	@Test
	public void testLocationStranded() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> qs = handler.parseQuery(
				"{\"location\":{\"seq_region_name\":\"chr1\",\"start\":\"2\",\"end\":\"10\",\"strand\":\"1\"}}");
		System.out.println(qs);
		assertEquals("Multi query", 4, qs.size());
		assertEquals("seq_region type", GeneQueryType.TERM, qs.get(0).getType());
		assertEquals("seq_region name", "chr1", qs.get(0).getValues()[0]);
		assertEquals("start name", GeneQueryType.RANGE, qs.get(1).getType());
		assertEquals("start type", new Long(2), qs.get(1).getStart());
		assertEquals("end name", GeneQueryType.RANGE, qs.get(2).getType());
		assertEquals("end type", new Long(10), qs.get(2).getEnd());
		assertEquals("end name", GeneQueryType.TERM, qs.get(3).getType());
		assertEquals("end type", "1", qs.get(3).getValues()[0]);
	}

	@Test
	public void testLocationStart() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> qs = handler.parseQuery("{\"location\":{\"seq_region_name\":\"chr1\",\"start\":\"2\"}}");
		System.out.println(qs);
		assertEquals("Multi query", 2, qs.size());
		assertEquals("seq_region type", GeneQueryType.TERM, qs.get(0).getType());
		assertEquals("seq_region name", "chr1", qs.get(0).getValues()[0]);
		assertEquals("start name", GeneQueryType.RANGE, qs.get(1).getType());
		assertEquals("start type", new Long(2), qs.get(1).getStart());
	}

	@Test
	public void testLocationEnd() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> qs = handler.parseQuery("{\"location\":{\"seq_region_name\":\"chr1\",\"end\":\"10\"}}");
		System.out.println(qs);
		assertEquals("Multi query", 2, qs.size());
		assertEquals("seq_region type", GeneQueryType.TERM, qs.get(0).getType());
		assertEquals("seq_region name", "chr1", qs.get(0).getValues()[0]);
		assertEquals("end name", GeneQueryType.RANGE, qs.get(1).getType());
		assertEquals("end type", new Long(10), qs.get(1).getEnd());
	}

	@Test
	public void testLargeTerms() throws IOException {
		QueryHandler handler = new DefaultQueryHandler();
		String json = ESTestServer.readGzipResource("/q08_human_swissprot_full.json.gz");
		List<GeneQuery> qs = handler.parseQuery(json);
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMergeQueriesSimple() {
		Map<String, Object> map = new HashMap<>();
		map.put("a.b", "1");
		map.put("a.c", "2");
		Map<String, Object> mergeQueries = DefaultQueryHandler.mergeQueries(map);
		assertEquals("Single key", 1, mergeQueries.keySet().size());
		assertTrue("a found", mergeQueries.containsKey("a"));
		Object aVal = mergeQueries.get("a");
		assertTrue("a is a map", Map.class.isAssignableFrom(aVal.getClass()));
		assertEquals("Two keys", 2, ((Map) aVal).keySet().size());
		assertEquals("b found", "1", ((Map) aVal).get("b"));
		assertEquals("c found", "2", ((Map) aVal).get("c"));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMergeQueriesMixed() {
		Map<String, Object> map = new HashMap<>();
		map.put("a.b", "1");
		map.put("x", "3");
		map.put("a.c", "2");
		Map<String, Object> mergeQueries = DefaultQueryHandler.mergeQueries(map);
		assertEquals("Two keys", 2, mergeQueries.keySet().size());
		assertEquals("x found", "3", mergeQueries.get("x"));
		assertTrue("a found", mergeQueries.containsKey("a"));
		Object aVal = mergeQueries.get("a");
		assertTrue("a is a map", Map.class.isAssignableFrom(aVal.getClass()));
		assertEquals("Two keys", 2, ((Map) aVal).keySet().size());
		assertEquals("b found", "1", ((Map) aVal).get("b"));
		assertEquals("c found", "2", ((Map) aVal).get("c"));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testMergeQueriesNested() {
		Map<String, Object> map = new HashMap<>();
		map.put("a.b.c", "1");
		map.put("a.b.d", "2");
		Map<String, Object> mergeQueries = DefaultQueryHandler.mergeQueries(map);
		System.out.println(mergeQueries);
		assertEquals("Single key", 1, mergeQueries.keySet().size());
		assertTrue("a found", mergeQueries.containsKey("a"));
		Object aVal = mergeQueries.get("a");
		assertTrue("a is a map", Map.class.isAssignableFrom(aVal.getClass()));
		assertEquals("One keys", 1, ((Map) aVal).keySet().size());
		assertTrue("b found", ((Map) aVal).containsKey("b"));
		Object bVal = ((Map) aVal).get("b");
		assertEquals("Two keys", 2, ((Map) bVal).keySet().size());
		assertEquals("b found", "1", ((Map) bVal).get("c"));
		assertEquals("c found", "2", ((Map) bVal).get("d"));
	}
	
	@Test
	public void testParseAndMerge() {
		QueryHandler handler = new DefaultQueryHandler();
		List<GeneQuery> qs = handler.parseQuery("{\"key.a\":\"1\",\"key.b\":\"2\"}");
		System.out.println(qs);
		assertEquals("Single query", 1, qs.size());
		GeneQuery q = qs.get(0);
		assertEquals("Query type", GeneQueryType.NESTED, q.getType());
		assertEquals("Query field", "key", q.getFieldName());
		assertEquals("Subqueries", 2, q.getSubQueries().length);
		GeneQuery subQ1 = q.getSubQueries()[0];
		assertEquals("Query type", GeneQueryType.TERM, subQ1.getType());
		assertEquals("Query field", "a", subQ1.getFieldName());
		assertEquals("Query value size", 1, subQ1.getValues().length);
		assertEquals("Query value", "1", subQ1.getValues()[0]);
		GeneQuery subQ2 = q.getSubQueries()[1];
		assertEquals("Query type", GeneQueryType.TERM, subQ2.getType());
		assertEquals("Query field", "b", subQ2.getFieldName());
		assertEquals("Query value size", 1, subQ2.getValues().length);
		assertEquals("Query value", "2", subQ2.getValues()[0]);
	}

}
