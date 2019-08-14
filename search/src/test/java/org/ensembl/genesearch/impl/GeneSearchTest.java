/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ensembl.genesearch.impl;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.ensembl.genesearch.*;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.ensembl.genesearch.utils.QueryHandlerTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author dstaines
 * @author mchakiachvili
 */
@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class GeneSearchTest {

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
    static GeneSearch geneSearch;

    @BeforeClass
    public static void setUp() throws IOException {
        // index a sample of JSON
        testServer = new ESTestServer();
        search = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE, geneInfo);
        gSearch = new ESSearch(testServer.getClient(), ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE,
                genomeInfo);
        provider = new SearchRegistry().registerSearch(SearchType.GENES, search)
                .registerSearch(SearchType.GENOMES, gSearch).registerSearch(SearchType.HOMOLOGUES, search);
        geneSearch = new GeneSearch(provider);
        log.info("Reading documents");
        String json = DataUtils.readGzipResource("/nanoarchaeum_equitans_kin4_m_genes.json.gz");
        String json2 = DataUtils.readGzipResource("/mycoplasma_pneumoniae_m129_genes.json.gz");
        String json3 = DataUtils.readGzipResource("/wolbachia_endosymbiont_of_drosophila_melanogaster_genes.json.gz");
        String gJson = DataUtils.readGzipResource("/genomes.json.gz");
        log.info("Creating test indices");
        testServer.indexTestDocs(json, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        testServer.indexTestDocs(json2, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        testServer.indexTestDocs(json3, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        testServer.indexTestDocs(gJson, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
    }

    @Test
    public void querySimple() {
        log.info("Querying for all genes");
        QueryResult result = geneSearch.query(Collections.emptyList(), QueryOutput.build(Collections.singletonList("id")),
                Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 2646, result.getResultCount());
        assertEquals("Fetched hits", 5, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
        assertTrue("id found", result.getResults().get(0).containsKey("id"));
        assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
    }

    @Test
    public void queryJoin() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
        QueryResult result = geneSearch.query(QueryHandlerTest.build("{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}"),
                o, Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 598, result.getResultCount());
        assertEquals("Fetched hits", 5, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
        assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
        Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes"))
                .findFirst();
        assertTrue("genomes found", genome.isPresent());
        Map<String, Object> genomes = (Map) genome.get().get("genomes");
        assertTrue("genomes.id found", genomes.containsKey("id"));
        assertTrue("genomes.name found", genomes.containsKey("name"));
        assertTrue("genomes.division found", genomes.containsKey("division"));
        assertEquals("Correct field info found", 5, result.getFields().size());
    }

    @Test
    public void queryJoinQueryFrom() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"id\":\"NEQ043\"}");
        QueryResult result = geneSearch.query(q, o, Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 1, result.getResultCount());
        assertEquals("Fetched hits", 1, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
        assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
        Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes"))
                .findFirst();
        assertTrue("genomes found", genome.isPresent());
        Map<String, Object> genomes = (Map) genome.get().get("genomes");
        assertTrue("genomes.id found", genomes.containsKey("id"));
        assertTrue("genomes.name found", genomes.containsKey("name"));
        assertTrue("genomes.division found", genomes.containsKey("division"));
    }

    @Test
    public void queryJoinQueryTo() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"id\":\"NEQ043\",\"genomes\":{\"division\":\"EnsemblBacteria\"}}");
        QueryResult result = geneSearch.query(q, o, Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 1, result.getResultCount());
        assertEquals("Fetched hits", 1, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
        assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
        Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes"))
                .findFirst();
        assertTrue("genomes found", genome.isPresent());
        Map<String, Object> genomes = (Map) genome.get().get("genomes");
        assertTrue("genomes.id found", genomes.containsKey("id"));
        assertTrue("genomes.name found", genomes.containsKey("name"));
        assertTrue("genomes.division found", genomes.containsKey("division"));
    }

    @Test
    public void queryJoinQueryToNone() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"id\":\"NEQ043\",\"genomes\":{\"division\":\"EnsemblFruit\"}}");
        QueryResult result = geneSearch.query(q, o, Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 1, result.getResultCount());
        assertEquals("Fetched hits", 1, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
        assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
        Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes"))
                .findFirst();
        assertFalse("genomes found", genome.isPresent());
    }

    @Test
    public void fetchSimple() {
        log.info("Fetching for all genes");
        SearchResult result = geneSearch.fetch(QueryHandlerTest.build("{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}"),
                QueryOutput.build(Collections.singletonList("id")));
        assertEquals("Total hits", 598, result.getResults().size());
        assertTrue("id found", result.getResults().get(0).containsKey("id"));
        assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
    }

    @Test
    public void fetchJoin() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
        SearchResult result = geneSearch.fetch(QueryHandlerTest.build("{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}"),
                o);
        assertEquals("Fetched hits", 598, result.getResults().size());
        assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
        Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes"))
                .findFirst();
        assertTrue("genomes found", genome.isPresent());
        Map<String, Object> genomes = (Map) genome.get().get("genomes");
        assertTrue("genomes.id found", genomes.containsKey("id"));
        assertTrue("genomes.name found", genomes.containsKey("name"));
        assertTrue("genomes.division found", genomes.containsKey("division"));
    }

    @Test
    public void fetchJoinQueryFrom() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"id\":\"NEQ043\"}");
        SearchResult result = geneSearch.fetch(q, o);
        assertEquals("Fetched hits", 1, result.getResults().size());
        assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
        Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes"))
                .findFirst();
        assertTrue("genomes found", genome.isPresent());
        Map<String, Object> genomes = (Map) genome.get().get("genomes");
        assertTrue("genomes.id found", genomes.containsKey("id"));
        assertTrue("genomes.name found", genomes.containsKey("name"));
        assertTrue("genomes.division found", genomes.containsKey("division"));
    }

    @Test
    public void fetchJoinQueryTo() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"id\":\"NEQ043\",\"genomes\":{\"division\":\"EnsemblBacteria\"}}");
        SearchResult result = geneSearch.fetch(q, o);
        assertEquals("Fetched hits", 1, result.getResults().size());
        assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
        Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes"))
                .findFirst();
        assertTrue("genomes found", genome.isPresent());
        Map<String, Object> genomes = (Map) genome.get().get("genomes");
        assertTrue("genomes.id found", genomes.containsKey("id"));
        assertTrue("genomes.name found", genomes.containsKey("name"));
        assertTrue("genomes.division found", genomes.containsKey("division"));
    }

    @Test
    public void fetchJoinQueryToNone() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"name\",\"division\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"id\":\"NEQ043\",\"genomes\":{\"division\":\"EnsemblFruit\"}}");
        SearchResult result = geneSearch.fetch(q, o);
        assertEquals("Fetched hits", 1, result.getResults().size());
        assertTrue("id found", result.getResults().stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("description found", result.getResults().stream().anyMatch(f -> f.containsKey("description")));
        Optional<Map<String, Object>> genome = result.getResults().stream().filter(f -> f.containsKey("genomes"))
                .findFirst();
        assertFalse("genomes found", genome.isPresent());
    }

    @Test
    public void queryJoinQueryHomologues() {
        log.info("Querying for all genes joining to genomes");
        QueryOutput o = QueryOutput
                .build("[\"name\",\"description\",{\"homologues\":[\"name\",\"genome\",\"seq_region_name\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"id\":\"NEQ519\"}");
        QueryResult result = geneSearch.query(q, o, Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 1, result.getResultCount());
        assertEquals("Fetched hits", 1, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
        Map<String, Object> gene = result.getResults().get(0);
        assertTrue("id found", gene.containsKey("id"));
        assertTrue("description found", gene.containsKey("description"));
        assertTrue("homologues found", gene.containsKey("homologues"));
        List<Map<String, Object>> homologs = (List) gene.get("homologues");
        Optional<Map<String, Object>> homolog = homologs.stream().filter(h -> h.containsKey("seq_region_name"))
                .findAny();
        assertTrue("Expanded homologue", homolog.isPresent());
        assertNotNull("Expanded homologue", homolog.get().get("genome"));
        assertNotNull("Expanded homologue", homolog.get().get("seq_region_name"));
    }

    @Test
    public void queryInnerJoinGenomes() {
        log.info("Querying for all genes inner join to genomes (should be N. equitans only)");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"division\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"biotype\":\"protein_coding\", \"genomes\":{\"inner\":\"1\"}}");
        QueryResult result = geneSearch.query(q, o, Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 536, result.getResultCount());
        assertEquals("Fetched hits", 5, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
        assertTrue("genomes found", result.getResults().stream().allMatch(g -> g.containsKey("genomes")));
    }

    @Test
    public void queryInnerJoinGenomesEmpty() {
        log.info("Querying for all genes inner join to genomes from EnsemblBanana (should be empty)");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"division\"]}]");
        List<Query> q = QueryHandlerTest
                .build("{\"biotype\":\"protein_coding\", \"genomes\":{\"inner\":\"1\", \"division\":\"banana\"}}");
        QueryResult result = geneSearch.query(q, o, Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 0, result.getResultCount());
        assertEquals("Fetched hits", 0, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
    }

    @Test
    public void fetchInnerJoinGenomes() {
        log.info("Querying for all genes inner join to genomes (should be N. equitans only)");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"division\"]}]");
        List<Query> q = QueryHandlerTest.build("{\"biotype\":\"protein_coding\", \"genomes\":{\"inner\":\"1\"}}");
        SearchResult result = geneSearch.fetch(q, o);
        assertEquals("Fetched hits", 536, result.getResults().size());
        assertTrue("genomes found", result.getResults().stream().allMatch(g -> g.containsKey("genomes")));
    }

    @Test
    public void fetchInnerJoinGenomesEmpty() {
        log.info("Querying for all genes inner join to genomes from EnsemblBanana (should be empty)");
        QueryOutput o = QueryOutput.build("[\"name\",\"description\",{\"genomes\":[\"division\"]}]");
        List<Query> q = QueryHandlerTest
                .build("{\"biotype\":\"protein_coding\", \"genomes\":{\"inner\":\"1\", \"division\":\"banana\"}}");
        SearchResult result = geneSearch.fetch(q, o);
        assertEquals("Fetched hits", 0, result.getResults().size());
    }

    @AfterClass
    public static void tearDown() {
        log.info("Disconnecting server");
        testServer.disconnect();
    }

}
