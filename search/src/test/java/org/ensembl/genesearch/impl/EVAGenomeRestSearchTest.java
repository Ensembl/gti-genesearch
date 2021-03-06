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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EVAGenomeRestSearchTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(WireMockConfiguration.options().dynamicPort());

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testGetGenomes() {
        EVAGenomeRestSearch search = new EVAGenomeRestSearch(wireMockRule.url(StringUtils.EMPTY), null);
        List<Map<String, Object>> genomes = search.getGenomes();
        assertEquals("Expected number of genomes", 21, genomes.size());
        Optional<Map<String, Object>> hsap = genomes.stream()
                .filter(g -> g.get("assemblyAccession").equals("GCA_000001405.1")).findAny();
        assertTrue("Homo sapiens found", hsap.isPresent());
        assertEquals("Correct assembly", "hsapiens", hsap.get().get("taxonomyCode"));
        assertEquals("Correct assembly", "grch37", hsap.get().get("assemblyCode"));
    }

    @Test
    public void testSelect() {
        EVAGenomeRestSearch search = new EVAGenomeRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/datatypes/evagenomes_datatype_info.json"));
        QueryResult result = search.select("GCA_000001405.1", 1, 10);
        assertEquals("Expected number of genomes", 1, result.getResultCount());
        Map<String, Object> hsap = result.getResults().get(0);
        assertTrue("Homo sapiens found", hsap != null);
        assertEquals("Correct assembly", "hsapiens", hsap.get("taxonomyCode"));
        assertEquals("Correct assembly", "grch37", hsap.get("assemblyCode"));
    }

    @Test
    public void testFetch() {
        EVAGenomeRestSearch search = new EVAGenomeRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/datatypes/evagenomes_datatype_info.json"));
        SearchResult result = search.fetch(
                Arrays.asList(new Query(FieldType.TERM, "assemblyAccession", "GCA_000001405.1")),
                new QueryOutput("assemblyAccession", "assemblyCode", "taxonomyCode"));
        assertEquals("Expected number of genomes", 1, result.getResults().size());
        Map<String, Object> hsap = result.getResults().get(0);
        assertTrue("Homo sapiens found", hsap != null);
        assertEquals("Correct assembly", "hsapiens", hsap.get("taxonomyCode"));
        assertEquals("Correct assembly", "grch37", hsap.get("assemblyCode"));
        assertTrue("Assembly version empty", !hsap.containsKey("assemblyVersion"));
        assertEquals("3 fields", 3, hsap.keySet().size());
    }

    @Test
    public void testQuery() {
        EVAGenomeRestSearch search = new EVAGenomeRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/datatypes/evagenomes_datatype_info.json"));
        QueryResult result = search.query(
                Arrays.asList(new Query(FieldType.TERM, "assemblyAccession", "GCA_000001405.1")),
                new QueryOutput("assemblyAccession", "assemblyCode", "taxonomyCode"), null, 1, 10, null);
        assertEquals("Expected number of genomes", 1, result.getResults().size());
        Map<String, Object> hsap = result.getResults().get(0);
        assertTrue("Homo sapiens found", hsap != null);
        assertEquals("Correct assembly", "hsapiens", hsap.get("taxonomyCode"));
        assertEquals("Correct assembly", "grch37", hsap.get("assemblyCode"));
        assertTrue("Assembly version empty", !hsap.containsKey("assemblyVersion"));
        assertEquals("3 fields", 3, hsap.keySet().size());
    }
}
