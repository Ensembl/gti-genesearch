package org.ensembl.genesearch;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

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
		List<GeneQuery> qs = handler
				.parseQuery("{\"location\":{\"seq_region_name\":\"chr1\",\"start\":\"2\",\"end\":\"10\",\"strand\":\"1\"}}");
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
}
