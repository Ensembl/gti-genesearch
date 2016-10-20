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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.impl.GeneSearch.SubSearchParams;
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
	static ESSearch search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);

	// set up a provider
	static SearchRegistry provider = new SearchRegistry().registerSearch(SearchType.GENES, search)
			.registerSearch(SearchType.HOMOLOGUES, null);

	// instantiate a join aware search
	static GeneSearch geneSearch = new GeneSearch(provider);

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
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch
				.decomposeQueryFields(Query.build("{\"name\":\"bob\",\"genome\":\"frank\"}"), QueryOutput.build(StringUtils.EMPTY));
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Query contains name",from.queries.stream().anyMatch(f->f.getFieldName().equals("name")));
		assertTrue("Query contains genome",from.queries.stream().anyMatch(f->f.getFieldName().equals("genome")));
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@Test
	public void decomposeFieldSimple() {
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch
				.decomposeQueryFields(Query.build(StringUtils.EMPTY), QueryOutput.build("[\"name\",\"genome\"]"));
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Fields contains name",from.fields.getFields().stream().anyMatch(f->f.equals("name")));
		assertTrue("Fields contains genome",from.fields.getFields().stream().anyMatch(f->f.equals("genome")));
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@Test
	public void decomposeQueryFieldSimple() {
		Pair<SubSearchParams, SubSearchParams> decomposeQueryFields = geneSearch
				.decomposeQueryFields(Query.build("{\"name\":\"bob\",\"genome\":\"frank\"}"), QueryOutput.build("[\"name\",\"genome\"]"));
		SubSearchParams from = decomposeQueryFields.getLeft();
		assertTrue("From name set", from.name.isPresent());
		assertTrue("Query contains name",from.queries.stream().anyMatch(f->f.getFieldName().equals("name")));
		assertTrue("Query contains genome",from.queries.stream().anyMatch(f->f.getFieldName().equals("genome")));
		assertTrue("Fields contains name",from.fields.getFields().stream().anyMatch(f->f.equals("name")));
		assertTrue("Fields contains genome",from.fields.getFields().stream().anyMatch(f->f.equals("genome")));
		SubSearchParams to = decomposeQueryFields.getRight();
		assertFalse("To name not set", to.name.isPresent());
	}

	@AfterClass
	public static void tearDown() {
		log.info("Disconnecting server");
		testServer.disconnect();
	}

}
