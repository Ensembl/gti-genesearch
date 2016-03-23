package org.ensembl.genesearch;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.ensembl.genesearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
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
		List<GeneQuery> q = handler
				.parseQuery("{\"key1\":\"1\",\"key2\":\"2\"}");
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
		List<GeneQuery> qs = handler
				.parseQuery("{\"key1\":{\"a\":\"1\",\"b\":\"2\"}}");
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

}
