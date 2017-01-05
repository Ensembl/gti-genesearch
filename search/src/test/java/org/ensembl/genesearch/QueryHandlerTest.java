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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.Test;

public class QueryHandlerTest {

	@Test
	public void testSimpleSingle() {
		QueryHandler handler = new DefaultQueryHandler();
		List<Query> q = handler.parseQuery("{\"key\":\"value\"}");
		assertEquals("Single query", 1, q.size());
		assertEquals("Query type", QueryType.TERM, q.get(0).getType());
		assertEquals("Query field", "key", q.get(0).getFieldName());
		assertEquals("Query value size", 1, q.get(0).getValues().length);
		assertEquals("Query value", "value", q.get(0).getValues()[0]);
	}

	@Test
	public void testSimpleMultiple() {
		QueryHandler handler = new DefaultQueryHandler();
		List<Query> q = handler.parseQuery("{\"key\":[\"1\",\"2\"]}");
		assertEquals("Single query", 1, q.size());
		assertEquals("Query type", QueryType.TERM, q.get(0).getType());
		assertEquals("Query field", "key", q.get(0).getFieldName());
		assertEquals("Query value size", 2, q.get(0).getValues().length);
		assertEquals("Query value", "1", q.get(0).getValues()[0]);
		assertEquals("Query value", "2", q.get(0).getValues()[1]);
	}

	@Test
	public void testDouble() {
		QueryHandler handler = new DefaultQueryHandler();
		List<Query> q = handler.parseQuery("{\"key1\":\"1\",\"key2\":\"2\"}");
		System.out.println(q);
		assertEquals("Double query", 2, q.size());
		assertEquals("Query type", QueryType.TERM, q.get(0).getType());
		assertEquals("Query field", "key1", q.get(0).getFieldName());
		assertEquals("Query value size", 1, q.get(0).getValues().length);
		assertEquals("Query value", "1", q.get(0).getValues()[0]);
		assertEquals("Query type", QueryType.TERM, q.get(1).getType());
		assertEquals("Query field", "key2", q.get(1).getFieldName());
		assertEquals("Query value size", 1, q.get(1).getValues().length);
		assertEquals("Query value", "2", q.get(1).getValues()[0]);
	}

	@Test
	public void testNested() {
		QueryHandler handler = new DefaultQueryHandler();
		List<Query> qs = handler.parseQuery("{\"key1\":{\"a\":\"1\",\"b\":\"2\"}}");
		System.out.println(qs);
		assertEquals("Single query", 1, qs.size());
		Query q = qs.get(0);
		assertEquals("Query type", QueryType.NESTED, q.getType());
		assertEquals("Query field", "key1", q.getFieldName());
		assertEquals("Subqueries", 2, q.getSubQueries().length);
		Query subQ1 = q.getSubQueries()[0];
		assertEquals("Query type", QueryType.TERM, subQ1.getType());
		assertEquals("Query field", "a", subQ1.getFieldName());
		assertEquals("Query value size", 1, subQ1.getValues().length);
		assertEquals("Query value", "1", subQ1.getValues()[0]);
		Query subQ2 = q.getSubQueries()[1];
		assertEquals("Query type", QueryType.TERM, subQ2.getType());
		assertEquals("Query field", "b", subQ2.getFieldName());
		assertEquals("Query value size", 1, subQ2.getValues().length);
		assertEquals("Query value", "2", subQ2.getValues()[0]);
	}

	@Test
	public void testLargeTerms() throws IOException {
		QueryHandler handler = new DefaultQueryHandler();
		String json = DataUtils.readGzipResource("/q08_human_swissprot_full.json.gz");
		List<Query> qs = handler.parseQuery(json);
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
		List<Query> qs = handler.parseQuery("{\"key.a\":\"1\",\"key.b\":\"2\"}");
		System.out.println(qs);
		assertEquals("Single query", 1, qs.size());
		Query q = qs.get(0);
		assertEquals("Query type", QueryType.NESTED, q.getType());
		assertEquals("Query field", "key", q.getFieldName());
		assertEquals("Subqueries", 2, q.getSubQueries().length);
		Query subQ1 = q.getSubQueries()[0];
		assertEquals("Query type", QueryType.TERM, subQ1.getType());
		assertEquals("Query field", "a", subQ1.getFieldName());
		assertEquals("Query value size", 1, subQ1.getValues().length);
		assertEquals("Query value", "1", subQ1.getValues()[0]);
		Query subQ2 = q.getSubQueries()[1];
		assertEquals("Query type", QueryType.TERM, subQ2.getType());
		assertEquals("Query field", "b", subQ2.getFieldName());
		assertEquals("Query value size", 1, subQ2.getValues().length);
		assertEquals("Query value", "2", subQ2.getValues()[0]);
	}

	@Test
	public void testExpandQuery() {
		List<String> ids = Arrays.asList("1", "2", "3");
		{
			Query q = Query.expandQuery("a", ids);
			assertEquals("a found","a",q.getFieldName());
			assertEquals("TERM query",QueryType.TERM,q.getType());
			assertEquals("Correct IDs found",ids.size(),q.getValues().length);
		}
		{
			Query q = Query.expandQuery("a.b", Arrays.asList("1", "2", "3"));
			assertEquals("a found","a",q.getFieldName());
			assertEquals("NESTED query",QueryType.NESTED,q.getType());
			Query q2 = q.getSubQueries()[0];
			assertEquals("b found","b",q2.getFieldName());
			assertEquals("TERM query",QueryType.TERM,q2.getType());
			assertEquals("Correct IDs found",ids.size(),q2.getValues().length);			
		}
		{
			Query q = Query.expandQuery("a.b.c", Arrays.asList("1", "2", "3"));
			assertEquals("a found","a",q.getFieldName());
			assertEquals("NESTED query",QueryType.NESTED,q.getType());
			Query q2 = q.getSubQueries()[0];
			assertEquals("b found","b",q2.getFieldName());
			assertEquals("NESTED query",QueryType.NESTED,q2.getType());
			Query q3 = q2.getSubQueries()[0];
			assertEquals("c found","c",q3.getFieldName());
			assertEquals("TERM query",QueryType.TERM,q3.getType());
			assertEquals("Correct IDs found",ids.size(),q3.getValues().length);			
		}
	}

}
