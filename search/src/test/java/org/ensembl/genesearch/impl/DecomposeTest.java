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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryHandlerTest;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.impl.JoinMergeSearch.SubSearchParams;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dstaines
 *
 */
public class DecomposeTest {

	static Logger log = LoggerFactory.getLogger(ESGeneSearchTest.class);

	static ESTestServer testServer;
	static DataTypeInfo geneInfo = DataTypeInfo.fromResource("/datatypes/genes_datatype_info.json");
	static DataTypeInfo genomeInfo = DataTypeInfo.fromResource("/datatypes/genomes_datatype_info.json");
	static DataTypeInfo homologueInfo = DataTypeInfo.fromResource("/datatypes/homologues_datatype_info.json");
	static ESSearch search;
	static ESSearch gSearch;

	// set up a provider
	static SearchRegistry provider;

	// instantiate a join aware search
	static JoinMergeSearch geneSearch;

	@BeforeClass
	public static void setUp() throws IOException {
		// index a sample of JSON
		testServer = new ESTestServer();
		search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE, geneInfo);
		gSearch = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENOME_ESTYPE, genomeInfo);
		provider = new SearchRegistry().registerSearch(SearchType.GENES, search)
				.registerSearch(SearchType.HOMOLOGUES, search).registerSearch(SearchType.GENOMES, gSearch);
		geneSearch = new GeneSearch(provider);
		log.info("Reading documents");
		String json = DataUtils.readGzipResource("/es_variants.json.gz");
		log.info("Creating test index");
		testServer.indexTestDocs(json, ESSearch.VARIANTS_INDEX, ESSearch.VARIANT_ESTYPE);
	}

	@Test
	public void decomposeEmpty() {
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch
				.decomposeQueryFields(QueryHandlerTest.build(StringUtils.EMPTY), QueryOutput.build(StringUtils.EMPTY));
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@Test
	public void decomposeQuerySimple() {
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch.decomposeQueryFields(
				QueryHandlerTest.build("{\"name\":\"bob\",\"genome\":\"frank\"}"), QueryOutput.build(StringUtils.EMPTY));
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Query contains name", from.queries.stream().anyMatch(f -> f.getFieldName().equals("name")));
		assertTrue("Query contains genome", from.queries.stream().anyMatch(f -> f.getFieldName().equals("genome")));
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@Test
	public void decomposeFieldSimple() {
		List<Query> q = QueryHandlerTest.build(StringUtils.EMPTY);
		QueryOutput o = QueryOutput.build("[\"name\",\"genome\"]");
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch.decomposeQueryFields(q, o);
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Fields contains name", from.fields.getFields().stream().anyMatch(f -> f.equals("name")));
		assertTrue("Fields contains genome", from.fields.getFields().stream().anyMatch(f -> f.equals("genome")));
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@Test
	public void decomposeQueryFieldSimple() {
		List<Query> q = QueryHandlerTest.build("{\"name\":\"bob\",\"genome\":\"frank\"}");
		QueryOutput o = QueryOutput.build("[\"name\",\"genome\"]");
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch.decomposeQueryFields(q, o);
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Query contains name", from.queries.stream().anyMatch(f -> f.getFieldName().equals("name")));
		assertTrue("Query contains genome", from.queries.stream().anyMatch(f -> f.getFieldName().equals("genome")));
		assertTrue("Fields contains name", from.fields.getFields().stream().anyMatch(f -> f.equals("name")));
		assertTrue("Fields contains genome", from.fields.getFields().stream().anyMatch(f -> f.equals("genome")));
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@Test
	public void decomposeFieldJoin() {
		List<Query> q = QueryHandlerTest.build(StringUtils.EMPTY);
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"display_name\",\"division\"]}]");
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch.decomposeQueryFields(q, o);
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Fields contains name", from.fields.getFields().stream().anyMatch(f -> f.equals("name")));
		assertTrue("Fields contains description",
				from.fields.getFields().stream().anyMatch(f -> f.equals("description")));
		assertTrue("Fields contains description", from.fields.getFields().stream().anyMatch(f -> f.equals("genome")));
		assertFalse("Fields contains null", from.fields.getFields().stream().anyMatch(f -> f == null));
		assertEquals("From key is genome", "genome", from.keys[0]);
		SubSearchParams to = decomposeQueryFields.getRight();
		assertEquals("To name set", SearchType.GENOMES.name(), to.name.get().name());
		assertTrue("Fields contains id", to.fields.getFields().stream().anyMatch(f -> f.equals("id")));
		assertTrue("Fields contains display_name",
				to.fields.getFields().stream().anyMatch(f -> f.equals("display_name")));
		assertTrue("Fields contains division", to.fields.getFields().stream().anyMatch(f -> f.equals("division")));
		assertEquals("To key is id", "id", to.keys[0]);
	}

	@Test
	public void decomposeQueryFieldJoin() {
		List<Query> q = QueryHandlerTest.build("{\"name\":\"bob\",\"genome\":\"frank\",\"genomes\":{\"display_name\":\"eric\"}}");
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"display_name\",\"division\"]}]");
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch.decomposeQueryFields(q, o);
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Query contains name", from.queries.stream().anyMatch(f -> f.getFieldName().equals("name")));
		assertTrue("Query contains genome", from.queries.stream().anyMatch(f -> f.getFieldName().equals("genome")));
		assertTrue("Fields contains name", from.fields.getFields().stream().anyMatch(f -> f.equals("name")));
		assertTrue("Fields contains description",
				from.fields.getFields().stream().anyMatch(f -> f.equals("description")));
		assertFalse("Fields contains null", from.fields.getFields().stream().anyMatch(f -> f == null));
		assertEquals("From key is genome", "genome", from.keys[0]);
		SubSearchParams to = decomposeQueryFields.getRight();
		assertEquals("To name set", SearchType.GENOMES.name(), to.name.get().name());
		assertTrue("Fields contains id", to.fields.getFields().stream().anyMatch(f -> f.equals("id")));
		assertTrue("Fields contains display_name",
				to.fields.getFields().stream().anyMatch(f -> f.equals("display_name")));
		assertTrue("Fields contains division", to.fields.getFields().stream().anyMatch(f -> f.equals("division")));
		assertTrue("Query contains display_name",
				to.queries.stream().anyMatch(f -> f.getFieldName().equals("display_name")));
		assertEquals("To key is id", "id", to.keys[0]);
	}

	@AfterClass
	public static void tearDown() throws IOException {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
