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

package org.ensembl.gti.genesearch.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.ensembl.gti.genesearch.services.Application;
import org.ensembl.gti.genesearch.services.EndpointSearchProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dstaines
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@WebIntegrationTest
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class EndpointTests {

    private static final String API_BASE = "http://localhost:8080/api";
    private static final String GENES_FETCH = API_BASE + "/genes/fetch";
    private static final String GENES_QUERY = API_BASE + "/genes/query";
    private static final String GENOMES_FETCH = API_BASE + "/genomes/fetch";
    private static final String GENOMES_QUERY = API_BASE + "/genomes/query";
    private static final String GENOMES_SELECT = API_BASE + "/genomes/select";
    private static final String TRANSCRIPTS_FETCH = API_BASE + "/transcripts/fetch";
    private static final String TRANSCRIPTS_QUERY = API_BASE + "/transcripts/query";
    private static final String INFO = API_BASE + "/genes/info";

    static Logger log = LoggerFactory.getLogger(EndpointTests.class);
    static ESSearch geneSearch;
    static ESSearch genomeSearch;
    static ESTestServer esTestServer;

    @BeforeClass
    public static void setUp() throws IOException {
        // create our ES test server once only
        log.info("Setting up");
        esTestServer = new ESTestServer();
        // index a sample of JSON
        log.info("Reading documents");
        String geneJson = DataUtils.readGzipResource("/nanoarchaeum_equitans_kin4_m_genes.json.gz");
        log.info("Creating test index");
        String genomeJson = DataUtils.readGzipResource("/genomes.json.gz");
        log.info("Creating test index");
        esTestServer.indexTestDocs(geneJson, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        esTestServer.indexTestDocs(genomeJson, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
        String variantJson = DataUtils.readGzipResource("/variants.json.gz");

    }

    @Autowired
    EndpointSearchProvider provider;

    public static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<Map<String, Object>>() {
    };
    public static final TypeReference<List<Map<String, Object>>> LIST_REF = new TypeReference<List<Map<String, Object>>>() {
    };
    public static final TypeReference<List<String>> STRING_LIST_REF = new TypeReference<List<String>>() {
    };

    RestTemplate restTemplate = new TestRestTemplate();

    @Before
    public void injectSearch() {
        // ensure we always use our test instances
        provider.setESClient(esTestServer.getClient());
    }

    @Test
    public void testQueryGetEndpoint() {
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, GENES_QUERY);
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        assertEquals("Checking limited results retrieved", 10, ((List<?>) result.get("results")).size());
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertTrue("ID found", results.get(0).containsKey("id"));
        assertTrue("Name found", results.get(0).containsKey("genome"));
        assertFalse("homologues not found", results.get(0).containsKey("homologues"));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) (result.get("fields"));
        assertEquals("ID found", "id", fields.get(0).get("name"));
    }

    @Test
    public void testQueryGetArrayEndpoint() {
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, GENES_QUERY + "?array=true");
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) result.get("fields");
        List<List<Object>> results = (List<List<Object>>) result.get("results");
        assertEquals("Checking limited results retrieved", 10, results.size());
        // check we have the same number
        int n = 0;
        int m = fields.size();
        for (List<Object> row : results) {
            assertEquals("Checking row " + (++n) + " has " + m + " columns", m, row.size());
        }
    }

    @Test
    public void testQueryPostEndpoint() {
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, GENES_QUERY, "{}");
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertEquals("Checking limited results retrieved", 10, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
        assertFalse("Name found", results.get(0).containsKey("name"));
        assertFalse("Genome found", results.get(0).containsKey("genome"));
        assertFalse("Homologues not found", results.get(0).containsKey("homologues"));
        Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
        assertTrue("Checking no facets retrieved", facets.isEmpty());
        List<Map<String, Object>> fields = (List<Map<String, Object>>) (result.get("fields"));
        assertEquals("ID found", "id", fields.get(0).get("name"));
    }

    @Test
    public void testFullQueryGetEndpoint() {
        String url = GENES_QUERY + "?query={query}" + "&limit=5" + "&fields=name,seq_region_name" + "&sort=+name,-start"
                + "&facets=biotype";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, url,
                "{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertEquals("Checking limited results retrieved", 5, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
        assertTrue("Name found", results.get(0).containsKey("name"));
        assertTrue("seq_region_name found", results.get(0).containsKey("seq_region_name"));
        assertFalse("homologues not found", results.get(0).containsKey("homologues"));
        Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
        assertEquals("Checking 1 facet retrieved", 1, facets.size());
        assertTrue("Checking facets populated", facets.containsKey("biotype"));
        assertEquals("Name found", "5_8S_rRNA", results.get(0).get("name"));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) (result.get("fields"));
        assertEquals("ID found", "id", fields.get(0).get("name"));
        assertEquals("name found", "name", fields.get(1).get("name"));
        assertEquals("seq_region_name found", "seq_region_name", fields.get(2).get("name"));
    }

    public void testOffsetQueryGetEndpoint() {
        String url1 = GENES_QUERY + "?query={query}" + "&limit=2" + "&fields=id";
        String url2 = GENES_QUERY + "?query={query}" + "&limit=2&offset=1" + "&fields=id";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> response1 = getUrlToObject(MAP_REF, restTemplate, url1,
                "{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
        Map<String, Object> response2 = getUrlToObject(MAP_REF, restTemplate, url2,
                "{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
        List<Map<String, Object>> results1 = (List<Map<String, Object>>) (response1.get("results"));
        List<Map<String, Object>> results2 = (List<Map<String, Object>>) (response2.get("results"));

        assertEquals("Got 2 results", 2, results1.size());

        log.info("Querying for all genes with offset");
        assertEquals("Got 2 results", 2, results2.size());
        assertTrue("Results 1.1 matches 2.0", results1.get(1).get("id").equals(results2.get(0).get("id")));
    }

    @Test
    public void testFullQueryPostEndpoint() {
        String paramJson = "{\"query\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"},"
                + "\"limit\":5,\"fields\":[\"name\",\"genome\",\"description\"]," + "\"sort\":[\"+name\",\"-start\"],"
                + "\"facets\":[\"biotype\"]}";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, GENES_QUERY, paramJson);
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertEquals("Checking limited results retrieved", 5, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
        assertTrue("Name found", results.get(0).containsKey("name"));
        assertFalse("homologues not found", results.get(0).containsKey("homologues"));
        Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
        assertEquals("Checking 1 facet retrieved", 1, facets.size());
        assertTrue("Checking facets populated", facets.containsKey("biotype"));
        assertEquals("Name found", "5_8S_rRNA", results.get(0).get("name"));
    }

    @Test
    public void testFetchGetEndpoint() {
        Map<String, Object> results = getUrlToObject(MAP_REF, restTemplate, GENES_FETCH);
        List<Map<String, Object>> result = (List<Map<String, Object>>) results.get("results");
        assertEquals("Checking all results found", 598, result.size());
        assertTrue("ID found", result.get(0).containsKey("id"));
        assertFalse("Homologues found", result.get(0).containsKey("homologues"));
        assertFalse("Transcripts found", result.get(0).containsKey("transcripts"));
    }

    @Test
    public void testFetchArrayGetEndpoint() {
        Map<String, Object> results = getUrlToObject(MAP_REF, restTemplate, GENES_FETCH + "?array=true");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) results.get("fields");
        List<List<Object>> result = (List<List<Object>>) results.get("results");
        assertEquals("Checking all results found", 598, result.size());
        // check we have the same number
        int n = 0;
        int m = fields.size();
        for (List<Object> row : result) {
            assertEquals("Checking row " + (++n) + " has " + m + " columns", m, row.size());
        }
    }

    @Test
    public void testFetchPostEndpoint() {
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, GENES_FETCH, "{}");
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
        assertEquals("Checking all results found", 598, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
        // assertFalse("homologues not found",
        // result.get(0).containsKey("homologues"));
    }

    @Test
    public void testFullFetchGetEndpoint() {
        String url = GENES_FETCH + "?query={query}" + "&fields=id,name,start";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> results = getUrlToObject(MAP_REF, restTemplate, url,
                "{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
        List<Map<String, Object>> result = (List<Map<String, Object>>) results.get("results");
        assertEquals("Checking all results found", 598, result.size());
        assertTrue("ID found", result.get(0).containsKey("id"));
        assertTrue("Start found", result.get(0).containsKey("start"));
        assertFalse("homologues not found", result.get(0).containsKey("homologues"));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) (results.get("fields"));
        assertEquals("ID found", "id", fields.get(0).get("name"));
    }

    @Test
    public void testFullFetchPostEndpoint() {
        String paramJson = "{\"query\":{\"genome\":\"nanoarchaeum_equitans_kin4_m\"},"
                + "\"fields\":[\"name\",\"genome\",\"start\"]}";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, GENES_FETCH, paramJson);
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
        assertEquals("Checking all results found", 598, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
        assertTrue("Start found", results.get(0).containsKey("start"));
        assertFalse("homologues not found", results.get(0).containsKey("homologues"));
    }

    @Test
    public void testTranscriptsQueryGetEndpoint() {
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, TRANSCRIPTS_QUERY);
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        assertEquals("Checking limited results retrieved", 10, ((List<?>) result.get("results")).size());
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertTrue("ID found", results.stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("Homologues not found", results.stream().anyMatch(f -> !f.containsKey("homologues")));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) (result.get("fields"));
        assertTrue("ID found in fields", fields.stream().anyMatch(f -> f.get("name").equals("id")));
    }

    @Test
    public void testTranscriptsQueryGetArrayEndpoint() {
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, TRANSCRIPTS_QUERY + "?array=true");
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) result.get("fields");
        List<List<Object>> results = (List<List<Object>>) result.get("results");
        assertEquals("Checking limited results retrieved", 10, results.size());
        // check we have the same number
        int n = 0;
        int m = fields.size();
        for (List<Object> row : results) {
            assertEquals("Checking row " + (++n) + " has " + m + " columns", m, row.size());
        }
    }

    @Test
    public void testTranscriptsQueryPostEndpoint() {
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, TRANSCRIPTS_QUERY,
                "{\"fields\":[\"id\",\"name\"]}");
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertEquals("Checking limited results retrieved", 10, results.size());
        assertTrue("ID found", results.stream().anyMatch(f -> f.containsKey("id")));
        assertTrue("Homologues found", results.stream().anyMatch(f -> !f.containsKey("homologues")));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) (result.get("fields"));
        assertTrue("ID found in fields", fields.stream().anyMatch(f -> f.get("name").equals("id")));
        Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
        assertTrue("Checking no facets retrieved", facets.isEmpty());
    }

    @Test
    public void testTranscriptsFullQueryGetEndpoint() {
        String url = GENES_QUERY + "?query={query}" + "&limit=5" + "&fields=name,seq_region_name" + "&sort=+name,-start"
                + "&facets=biotype";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, url,
                "{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
        assertEquals("Checking all results found", 598, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertEquals("Checking limited results retrieved", 5, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
        assertTrue("Name found", results.get(0).containsKey("name"));
        assertTrue("seq_region_name found", results.get(0).containsKey("seq_region_name"));
        assertFalse("homologues not found", results.get(0).containsKey("homologues"));
        Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
        assertEquals("Checking 1 facet retrieved", 1, facets.size());
        assertTrue("Checking facets populated", facets.containsKey("biotype"));
        assertEquals("Name found", "5_8S_rRNA", results.get(0).get("name"));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) (result.get("fields"));
        assertEquals("ID found", "id", fields.get(0).get("name"));
        assertEquals("name found", "name", fields.get(1).get("name"));
        assertEquals("seq_region_name found", "seq_region_name", fields.get(2).get("name"));
    }

    public void testTranscriptsOffsetQueryGetEndpoint() {
        String url1 = TRANSCRIPTS_QUERY + "?query={query}" + "&limit=2" + "&fields=id";
        String url2 = TRANSCRIPTS_QUERY + "?query={query}" + "&limit=2&offset=1" + "&fields=id";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> response1 = getUrlToObject(MAP_REF, restTemplate, url1,
                "{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
        Map<String, Object> response2 = getUrlToObject(MAP_REF, restTemplate, url2,
                "{\"genome\":\"nanoarchaeum_equitans_kin4_m\"}");
        List<Map<String, Object>> results1 = (List<Map<String, Object>>) (response1.get("results"));
        List<Map<String, Object>> results2 = (List<Map<String, Object>>) (response2.get("results"));

        assertEquals("Got 2 results", 2, results1.size());

        log.info("Querying for all genes with offset");
        assertEquals("Got 2 results", 2, results2.size());
        assertTrue("Results 1.1 matches 2.0", results1.get(1).get("id").equals(results2.get(0).get("id")));
    }

    @Test
    public void testTranscriptsFullQueryPostEndpoint() {
        String paramJson = "{\"query\":{\"biotype\":\"protein_coding\"},"
                + "\"limit\":5,\"fields\":[\"id\",\"name\",\"biotype\",\"description\"],"
                + "\"sort\":[\"+genes.name\",\"-start\"]," + "\"facets\":[\"biotype\"]}";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, TRANSCRIPTS_QUERY, paramJson);
        assertEquals("Checking all results found", 536, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertEquals("Checking limited results retrieved", 5, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
        assertTrue("Biotype found", results.get(0).containsKey("biotype"));
        assertFalse("homologues not found", results.get(0).containsKey("homologues"));
        Map<String, Object> facets = (Map<String, Object>) (result.get("facets"));
        assertEquals("Checking 1 facet retrieved", 1, facets.size());
        assertTrue("Checking facets populated", facets.containsKey("biotype"));
        // TODO requires sorting to be fixed first
        // assertEquals("Name found", "5S_rRNA", results.get(0).get("name"));
    }

    @Test
    public void testTranscriptsFetchGetEndpoint() {
        Map<String, Object> results = getUrlToObject(MAP_REF, restTemplate, TRANSCRIPTS_FETCH);
        List<Map<String, Object>> result = (List<Map<String, Object>>) results.get("results");
        assertEquals("Checking all results found", 598, result.size());
        assertTrue("ID found", result.get(0).containsKey("id"));
        assertFalse("Homologues found", result.get(0).containsKey("homologues"));
        assertFalse("Transcripts found", result.get(0).containsKey("transcripts"));
    }

    @Test
    public void testTranscriptsFetchArrayGetEndpoint() {
        Map<String, Object> results = getUrlToObject(MAP_REF, restTemplate, TRANSCRIPTS_FETCH + "?array=true");
        List<Map<String, Object>> fields = (List<Map<String, Object>>) results.get("fields");
        List<List<Object>> result = (List<List<Object>>) results.get("results");
        assertEquals("Checking all results found", 598, result.size());
        // check we have the same number
        int n = 0;
        int m = fields.size();
        for (List<Object> row : result) {
            assertEquals("Checking row " + (++n) + " has " + m + " columns", m, row.size());
        }
    }

    @Test
    public void testTranscriptsFetchPostEndpoint() {
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, TRANSCRIPTS_FETCH, "{}");
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
        assertEquals("Checking all results found", 598, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
    }

    @Test
    public void testTranscriptsFullFetchGetEndpoint() {
        String url = TRANSCRIPTS_FETCH + "?query={query}" + "&fields=id,name,start";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> results = getUrlToObject(MAP_REF, restTemplate, url, "{}");
        List<Map<String, Object>> result = (List<Map<String, Object>>) results.get("results");
        assertEquals("Checking all results found", 598, result.size());
        assertTrue("ID found", result.get(0).containsKey("id"));
        assertTrue("Start found", result.get(0).containsKey("start"));
        assertFalse("homologues not found", result.get(0).containsKey("homologues"));
        List<Map<String, Object>> fields = (List<Map<String, Object>>) (results.get("fields"));
        assertEquals("ID found", "id", fields.get(0).get("name"));
    }

    @Test
    public void testTranscriptsFullFetchPostEndpoint() {
        String paramJson = "{\"query\":{}," + "\"fields\":[\"id\",\"name\",\"start\"]}";
        // rest template expands {} as variables so supply JSON separately
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, TRANSCRIPTS_FETCH, paramJson);
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
        assertEquals("Checking all results found", 598, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
        assertTrue("Start found", results.get(0).containsKey("start"));
        assertFalse("homologues not found", results.get(0).containsKey("homologues"));
    }

    @Test
    public void testGenomeQueryGetEndpoint() {
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, GENOMES_QUERY);
        assertEquals("Checking all results found", 4, Long.parseLong(result.get("resultCount").toString()));
        assertEquals("Checking limited results retrieved", 4, ((List<?>) result.get("results")).size());
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertTrue("ID found", results.get(0).containsKey("id"));
    }

    @Test
    public void testGenomeQueryPostEndpoint() {
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, GENOMES_QUERY, "{}");
        assertEquals("Checking all results found", 4, Long.parseLong(result.get("resultCount").toString()));
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertEquals("Checking limited results retrieved", 4, results.size());
        assertTrue("ID found", results.get(0).containsKey("id"));
    }

    @Test
    public void testGenomeFetchGetEndpoint() {
        Map<String, Object> results = getUrlToObject(MAP_REF, restTemplate, GENOMES_FETCH);
        List<Map<String, Object>> result = (List<Map<String, Object>>) results.get("results");
        assertEquals("Checking all results found", 4, result.size());
        assertTrue("ID found", result.get(0).containsKey("id"));
    }

    @Test
    public void testGenomeFetchPostEndpoint() {
        Map<String, Object> results = postUrlToObject(MAP_REF, restTemplate, GENOMES_FETCH, "{}");
        List<Map<String, Object>> result = (List<Map<String, Object>>) results.get("results");
        assertEquals("Checking all results found", 4, result.size());
        assertTrue("ID found", result.get(0).containsKey("id"));
    }

    @Test
    public void testSelect() {
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, GENOMES_SELECT + "?query=human");
        assertEquals("Checking all results found", 2, Long.parseLong(result.get("resultCount").toString()));
        assertEquals("Checking limited results retrieved", 2, ((List<?>) result.get("results")).size());
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertTrue("ID found", results.get(0).containsKey("id"));
    }

    @Test
    public void testInfo() {

        Map<String, Object> type = getUrlToObject(MAP_REF, restTemplate, INFO);
        assertTrue("Name found", type.containsKey("name"));
        assertTrue("Targets found", type.containsKey("targets"));
        assertTrue("Fields found", type.containsKey("fieldInfo"));

        List<Map<String, Object>> fields = getUrlToObject(LIST_REF, restTemplate, INFO + "/fields");
        assertEquals("Checking number of fields", ((List) type.get("fieldInfo")).size(), fields.size());

        List<Map<String, Object>> facetFields = getUrlToObject(LIST_REF, restTemplate, INFO + "/fields?type=facet");
        facetFields.stream().anyMatch(f -> f.get("facet").equals("true"));
        List<Map<String, Object>> strandFields = getUrlToObject(LIST_REF, restTemplate, INFO + "/fields?type=strand");
        strandFields.stream().anyMatch(f -> f.get("type").equals("STRAND"));

    }

    /**
     * Helper method for invoking a URI as GET and parsing the result into a
     * hash
     * 
     * @param url
     *            URL template
     * @param params
     *            bind parameters for URL
     * @return object retrieved from GET
     */
    public static <T> T getUrlToObject(TypeReference<T> type, RestTemplate restTemplate, String url, Object... params) {
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, params);
        log.info("Get response: " + response.getBody());
        T map = null;
        try {
            map = new ObjectMapper().readValue(response.getBody(), type);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        return map;
    }

    /**
     * Helper method to invoke a JSON POST method with the supplied object and
     * then return the resulting object
     * 
     * @param restTemplate
     * @param url
     *            URL
     * @param json
     *            object to post
     * @param params
     *            URL bind params
     * @return object retrieved from POST
     */
    public static <T> T postUrlToObject(TypeReference<T> type, RestTemplate restTemplate, String url, String json,
            Object... params) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            log.trace("Invoking " + url + " with " + json);
            HttpEntity<String> entity = new HttpEntity<String>(json, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.trace("Post response: " + response.getBody());
            return new ObjectMapper().readValue(response.getBody(), type);

        } catch (IOException e) {
            fail(e.getMessage());
            return null;
        }

    }

}
