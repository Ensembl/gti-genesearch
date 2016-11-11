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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.impl.JoinMergeSearch.SubSearchParams;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.test.ESTestServer;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dstaines
 *
 */
public class DecomposeTest {

	static Logger log = LoggerFactory.getLogger(ESGeneSearchTest.class);

	static ESTestServer testServer = new ESTestServer();
	static DataTypeInfo geneInfo = DataTypeInfo.fromResource("/genes_datatype_info.json");
	static DataTypeInfo genomeInfo = DataTypeInfo.fromResource("/genomes_datatype_info.json");
	static DataTypeInfo homologueInfo = DataTypeInfo.fromResource("/homologues_datatype_info.json");
	static ESSearch search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE, geneInfo);
	static ESSearch gSearch = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENOME_ESTYPE, genomeInfo);

	// set up a provider
	static SearchRegistry provider = new SearchRegistry().registerSearch(SearchType.GENES, search)
			.registerSearch(SearchType.HOMOLOGUES, search).registerSearch(SearchType.GENOMES, gSearch);

	// instantiate a join aware search
	static JoinMergeSearch geneSearch = new GeneSearch(provider);

	@Test
	public void decomposeEmpty() {
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch
				.decomposeQueryFields(Query.build(StringUtils.EMPTY), QueryOutput.build(StringUtils.EMPTY));
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@Test
	public void decomposeQuerySimple() {
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch.decomposeQueryFields(
				Query.build("{\"name\":\"bob\",\"genome\":\"frank\"}"), QueryOutput.build(StringUtils.EMPTY));
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Query contains name", from.queries.stream().anyMatch(f -> f.getFieldName().equals("name")));
		assertTrue("Query contains genome", from.queries.stream().anyMatch(f -> f.getFieldName().equals("genome")));
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@Test
	public void decomposeFieldSimple() {
		List<Query> q = Query.build(StringUtils.EMPTY);
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
		List<Query> q = Query.build("{\"name\":\"bob\",\"genome\":\"frank\"}");
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
		List<Query> q = Query.build(StringUtils.EMPTY);
		QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"display_name\",\"division\"]}]");
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch.decomposeQueryFields(q, o);
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Fields contains name", from.fields.getFields().stream().anyMatch(f -> f.equals("name")));
		assertTrue("Fields contains description",
				from.fields.getFields().stream().anyMatch(f -> f.equals("description")));
		assertTrue("Fields contains description", from.fields.getFields().stream().anyMatch(f -> f.equals("genome")));
		assertFalse("Fields contains null", from.fields.getFields().stream().anyMatch(f -> f == null));
		assertEquals("From key is genome", "genome", from.key);
		SubSearchParams to = decomposeQueryFields.getRight();
		assertEquals("To name set", SearchType.GENOMES.name(), to.name.get().name());
		assertTrue("Fields contains id", to.fields.getFields().stream().anyMatch(f -> f.equals("id")));
		assertTrue("Fields contains display_name",
				to.fields.getFields().stream().anyMatch(f -> f.equals("display_name")));
		assertTrue("Fields contains division", to.fields.getFields().stream().anyMatch(f -> f.equals("division")));
		assertEquals("To key is id", "id", to.key);
	}

	@Test
	public void decomposeQueryFieldJoin() {
		List<Query> q = Query.build("{\"name\":\"bob\",\"genome\":\"frank\",\"genomes\":{\"display_name\":\"eric\"}}");
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
		assertEquals("From key is genome", "genome", from.key);
		SubSearchParams to = decomposeQueryFields.getRight();
		assertEquals("To name set", SearchType.GENOMES.name(), to.name.get().name());
		assertTrue("Fields contains id", to.fields.getFields().stream().anyMatch(f -> f.equals("id")));
		assertTrue("Fields contains display_name",
				to.fields.getFields().stream().anyMatch(f -> f.equals("display_name")));
		assertTrue("Fields contains division", to.fields.getFields().stream().anyMatch(f -> f.equals("division")));
		assertTrue("Query contains display_name",
				to.queries.stream().anyMatch(f -> f.getFieldName().equals("display_name")));
		assertEquals("To key is id", "id", to.key);
	}

	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
