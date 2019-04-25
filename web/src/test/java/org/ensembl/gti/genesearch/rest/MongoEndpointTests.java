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

package org.ensembl.gti.genesearch.rest;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.test.MongoTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.ensembl.gti.genesearch.services.Application;
import org.ensembl.gti.genesearch.services.EVAMongoEndpointProvider;
import org.ensembl.gti.genesearch.services.EndpointSearchProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.ensembl.gti.genesearch.rest.EndpointTests.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author dstaines
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes=Application.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("eva_mongo")
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class MongoEndpointTests {

    private static final String API_BASE = "http://localhost:8080/api";
    private static final String VARIANTS_FETCH = API_BASE + "/variants/fetch";
    private static final String VARIANTS_QUERY = API_BASE + "/variants/query";

    static Logger log = LoggerFactory.getLogger(MongoEndpointTests.class);
    static ESSearch geneSearch;
    static ESSearch genomeSearch;
    static ESTestServer esTestServer;
    static MongoCollection<Document> mongoCollection;
    static MongoTestServer mongoTestServer;

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
        mongoTestServer = new MongoTestServer();
        mongoTestServer.indexData(variantJson);
    }

    @Autowired
    EndpointSearchProvider provider;

    RestTemplate restTemplate = new TestRestTemplate().getRestTemplate();

    @Before
    public void injectSearch() {
        // ensure we always use our test instances
        provider.setESClient(esTestServer.getClient());
        ((EVAMongoEndpointProvider) provider).setMongoCollection(mongoTestServer.getCollection());
    }

    @Test
    public void testVariantQueryGetEndpoint() {
        Map<String, Object> result = getUrlToObject(MAP_REF, restTemplate, VARIANTS_QUERY);
        assertEquals("Checking limited results retrieved", 10, ((List<?>) result.get("results")).size());
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertTrue("ID found", results.get(0).containsKey("_id"));
    }

    @Test
    public void testVariantQueryPostEndpoint() {
        Map<String, Object> result = postUrlToObject(MAP_REF, restTemplate, VARIANTS_QUERY, "{}");
        List<Map<String, Object>> results = (List<Map<String, Object>>) (result.get("results"));
        assertEquals("Checking limited results retrieved", 10, results.size());
        assertTrue("ID found", results.get(0).containsKey("_id"));
    }

    @Test
    public void testVariantFetchGetEndpoint() {
        Map<String, Object> results = getUrlToObject(MAP_REF, restTemplate, VARIANTS_FETCH);
        List<Map<String, Object>> result = (List<Map<String, Object>>) results.get("results");
        assertEquals("Checking all results found", 10, result.size());
        assertTrue("ID found", result.get(0).containsKey("_id"));
    }

    @Test
    public void testVariantFetchPostEndpoint() {
        Map<String, Object> results = postUrlToObject(MAP_REF, restTemplate, VARIANTS_FETCH, "{}");
        List<Map<String, Object>> result = (List<Map<String, Object>>) results.get("results");
        assertEquals("Checking all results found", 10, result.size());
        assertTrue("ID found", result.get(0).containsKey("_id"));
    }

}
