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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.impl.ESSearchBuilder;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
import org.ensembl.genesearch.test.ESTestServer;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unchecked")
public class ESGeneSearchBuilderTest {

	@Test
	public void testId() {
		QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, new Query(QueryType.TERM, "id", "DDB0231518"));

		Map<String, Object> obj = jsonToMap(builder.toString());
		System.out.println(obj);
		assertObjCorrect("Object string check", "{constant_score={filter={ids={type=gene, values=[DDB0231518]}}}}",
				obj);
	}

	@Test
	public void testNestedHomology() {
		Query genome = new Query(QueryType.TERM, "genome", "dictyostelium_fasciculatum");
		Query orthology = new Query(QueryType.TERM, "description", "ortholog_one2one");
		Query homology = new Query(QueryType.NESTED, "homologues", genome, orthology);

		QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, homology);

		Map<String, Object> obj = jsonToMap(builder.toString());
		System.out.println(obj);

		assertTrue("Nested set", obj.containsKey("nested"));
		Map<String, Object> nested = (Map<String, Object>) obj.get("nested");
		assertEquals("Path", "homologues", nested.get("path"));
		assertTrue("Query set", nested.containsKey("query"));
		Map<String, Object> query = (Map<String, Object>) nested.get("query");
		assertTrue("Bool set", query.containsKey("bool"));

		assertObjCorrect("Object string check",
				"{nested={query={bool={must=[{constant_score={filter={term={homologues.genome=dictyostelium_fasciculatum}}}},"
						+ "{constant_score={filter={term={homologues.description=ortholog_one2one}}}}]}},path=homologues}}",
				obj);
	}

	@Test
	public void testNestedTranslationId() {
		Query idQuery = new Query(QueryType.TERM, "id", "DDB0231518");
		Query translationQuery = new Query(QueryType.NESTED, "translations", idQuery);
		Query geneQuery = new Query(QueryType.NESTED, "transcripts", translationQuery);
		QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, geneQuery);
		Map<String, Object> obj = jsonToMap(builder.toString());
		System.out.println(obj);
		assertTrue("Nested set", obj.containsKey("nested"));

		assertObjCorrect("Object string check",
				"{nested={query={nested={query={constant_score={filter={term={transcripts.translations.id=DDB0231518}}}}, "
						+ "path=transcripts.translations}}, path=transcripts}}",
				obj);

	}

	@Test
	public void testSimpleFacet() {
		AbstractAggregationBuilder buildAggregation = ESSearchBuilder.buildAggregation("GO",10);
		assertEquals("Class check", TermsBuilder.class, buildAggregation.getClass());
	}

	@Test
	public void testNestedFacet() {
		AbstractAggregationBuilder buildAggregation = ESSearchBuilder.buildAggregation("homologues.genome",10);
		assertEquals("Class check", NestedBuilder.class, buildAggregation.getClass());
	}

	@Test
	public void testDoubleNestedFacet() {
		AbstractAggregationBuilder buildAggregation = ESSearchBuilder.buildAggregation("homologues.genome.banana",10);
		assertEquals("Class check", NestedBuilder.class, buildAggregation.getClass());
	}

	@Test
	public void testRange() {
		Query seqRegion = new Query(QueryType.TERM, "seq_region_name", "DDB0231518");
		Query start = new Query(QueryType.RANGE, "start", (long) 1, null);
		Query end = new Query(QueryType.RANGE, "end", null, (long) 100);
		QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, seqRegion, start, end);
		Map<String, Object> obj = jsonToMap(builder.toString());
		System.out.println(obj);
		assertTrue("Bool set", obj.containsKey("bool"));
		Map<String, Object> bool = (Map<String, Object>) obj.get("bool");
		assertTrue("Values set", bool.containsKey("must"));
		assertObjCorrect("Object string check",
				"[{constant_score={filter={term={seq_region_name=DDB0231518}}}}, "
						+ "{constant_score={filter={range={start={from=1, to=null, include_lower=true, include_upper=true}}}}}, "
						+ "{constant_score={filter={range={end={from=null, to=100, include_lower=true, include_upper=true}}}}}]",
				bool.get("must"));
	}

	@Test
	public void testLargeTerms() throws IOException {
		QueryHandler handler = new DefaultQueryHandler();
		String json = ESTestServer.readGzipResource("/q08_human_swissprot_full.json.gz");
		List<Query> qs = handler.parseQuery(json);
		QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, qs.get(0));
		Map<String, Object> obj = jsonToMap(builder.toString());
		System.out.println(obj);
		assertTrue("Constant_score set", obj.containsKey("constant_score"));
		Map<String, Object> constant = (Map<String, Object>) obj.get("constant_score");
		assertTrue("filter set", constant.containsKey("filter"));
		Map<String, Object> filter = (Map<String, Object>) constant.get("filter");
		assertTrue("Terms set", filter.containsKey("terms"));
		Map<String, Object> terms = (Map<String, Object>) filter.get("terms");
		assertTrue("Uniprot_SWISSPROT set", terms.containsKey("Uniprot_SWISSPROT"));
		List<String> uniprot = (List<String>) (terms.get("Uniprot_SWISSPROT"));
		assertEquals("Uniprot_SWISSPROT size", 18920, uniprot.size());
	}

	protected Map<String, Object> jsonToMap(String json) {
		try {
			return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected static void assertObjCorrect(String message, String expected, Object obj) {
		String actual = obj.toString().replaceAll("\\s+", "");
		expected = expected.replaceAll("\\s+", "");
		assertEquals(message, expected, actual);
	}

}
