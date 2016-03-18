package org.ensembl.genesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.ensembl.genesearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.impl.ESGeneSearchBuilder;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
public class ESGeneSearchBuilderTest {

	@Test
	public void testId() {
		QueryBuilder builder = ESGeneSearchBuilder.buildQuery(new GeneQuery(
				GeneQueryType.TERM, "_id", "DDB0231518"));

		Map<String, Object> obj = jsonToMap(builder.toString());
		System.out.println(obj);
		assertTrue("Ids set", obj.containsKey("ids"));
		Map<String, Object> ids = (Map<String, Object>) obj.get("ids");
		assertTrue("Values set", ids.containsKey("values"));
		assertTrue("Values length 1",
				((Collection<?>) ids.get("values")).size() == 1);
		assertEquals("Type correct", "gene", ids.get("type"));

		assertObjCorrect("Object string check",
				"{ids={type=gene, values=[DDB0231518]}}", obj);
	}

	@Test
	public void testNestedHomology() {
		GeneQuery genome = new GeneQuery(GeneQueryType.TERM, "genome",
				"dictyostelium_fasciculatum");
		GeneQuery orthology = new GeneQuery(GeneQueryType.TERM, "description",
				"ortholog_one2one");
		GeneQuery homology = new GeneQuery(GeneQueryType.NESTED, "homologues",
				genome, orthology);

		QueryBuilder builder = ESGeneSearchBuilder.buildQuery(homology);

		Map<String, Object> obj = jsonToMap(builder.toString());
		System.out.println(obj);

		assertTrue("Nested set", obj.containsKey("nested"));
		Map<String, Object> nested = (Map<String, Object>) obj.get("nested");
		assertEquals("Path", "homologues", nested.get("path"));
		assertTrue("Query set", nested.containsKey("query"));
		Map<String, Object> query = (Map<String, Object>) nested.get("query");
		assertTrue("Bool set", query.containsKey("bool"));

		assertObjCorrect(
				"Object string check",
				"{nested={query={"
						+ "bool={must=[{term={homologues.genome=dictyostelium_fasciculatum}}, "
						+ "{term={homologues.description=ortholog_one2one}}]}}, path=homologues}}",
				obj);
	}

	@Test
	public void testNestedTranslationId() {
		GeneQuery idQuery = new GeneQuery(GeneQueryType.TERM, "id",
				"DDB0231518");
		GeneQuery translationQuery = new GeneQuery(GeneQueryType.NESTED,
				"translations", idQuery);
		GeneQuery geneQuery = new GeneQuery(GeneQueryType.NESTED,
				"transcripts", translationQuery);
		QueryBuilder builder = ESGeneSearchBuilder.buildQuery(geneQuery);
		Map<String, Object> obj = jsonToMap(builder.toString());
		System.out.println(obj);
		assertTrue("Nested set", obj.containsKey("nested"));

		assertObjCorrect("Object string check", "{nested={query={nested="
				+ "{query={term={transcripts.translations.id=DDB0231518}}, "
				+ "path=transcripts.translations}}, path=transcripts}}", obj);

	}
	
	@Test
	public void testSimpleFacet() {
		AbstractAggregationBuilder buildAggregation = ESGeneSearchBuilder.buildAggregation("GO");
		assertEquals("Class check", TermsBuilder.class, buildAggregation.getClass());
	}

	@Test
	public void testNestedFacet() {
		AbstractAggregationBuilder buildAggregation = ESGeneSearchBuilder.buildAggregation("homologues.genome");
		assertEquals("Class check", NestedBuilder.class, buildAggregation.getClass());
	}
	
	@Test
	public void testDoubleNestedFacet() {
		AbstractAggregationBuilder buildAggregation = ESGeneSearchBuilder.buildAggregation("homologues.genome.banana");
		assertEquals("Class check", NestedBuilder.class, buildAggregation.getClass());
	}

	protected Map<String, Object> jsonToMap(String json) {
		try {
			return new ObjectMapper().readValue(json,
					new TypeReference<Map<String, Object>>() {
					});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static void assertObjCorrect(String message, String expected,
			Object obj) {
		String actual = obj.toString().replaceAll("\\s+", "");
		expected = expected.replaceAll("\\s+", "");
		assertEquals(message, expected, actual);
	}

}
