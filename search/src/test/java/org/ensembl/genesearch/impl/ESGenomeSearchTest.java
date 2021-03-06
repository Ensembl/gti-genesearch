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
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class ESGenomeSearchTest extends AbstractESTestCase {

    static ESSearch search;

    @BeforeClass
    public static void initData() throws IOException {
        // index a sample of JSON
        search = new ESSearch(esTestClient.getClient(), ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE,
                DataTypeInfo.fromResource("/datatypes/genomes_datatype_info.json"));
        log.info("Reading documents");
        String json = DataUtils.readGzipResource("/genomes.json.gz");
        esTestClient.indexTestDocs(json, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fetchAll() {
        log.info("Fetching all genomes");
        search.fetch(Collections.emptyList(), QueryOutput.build(Collections.singletonList("_id")));
    }

    @Test
    public void fetchGenomeById() {
        log.info("Fetching genomes from genome");
        SearchResult result = search.fetch(Collections.singletonList(new Query(FieldType.TERM, "id", "homo_sapiens")),
                QueryOutput.build(Collections.singletonList("_id")));
        List<Map<String, Object>> ids = result.getResults();
        log.info("Fetched " + ids.size() + " genomes");
        assertEquals("Number of genomes", 1, ids.size());
    }

    @Test
    public void querySimple() {
        log.info("Querying for all genomes");
        QueryResult result = search.query(Collections.emptyList(), QueryOutput.build(Collections.singletonList("id")),
                Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Total hits", 5, result.getResultCount());
        assertEquals("Fetched hits", 5, result.getResults().size());
        assertEquals("Total facets", 0, result.getFacets().size());
        assertTrue("id found", result.getResults().get(0).containsKey("id"));
        assertEquals("1 field only", 1, result.getResults().get(0).keySet().size());
    }

    @Test
    public void fetchGenome() {
        log.info("Fetching a single genome");
        String id = "homo_sapiens";
        Map<String, Object> genome = search.fetchById(id);
        assertNotNull("Genome is not null", genome);
        assertEquals("ID correct", id, genome.get("id"));
    }

    @Test
    public void fetchGenomes() {
        log.info("Fetching list of genomes");
        String id1 = "homo_sapiens";
        String id2 = "escherichia_coli_str_k_12_substr_mg1655";
        List<Map<String, Object>> genomes = search.fetchByIds(id1, id2);
        assertNotNull("genomes are not null", genomes);
        assertEquals("2 genomes found", 2, genomes.size());
        Set<String> ids = genomes.stream().map(gene -> (String) gene.get("id")).collect(Collectors.toSet());
        assertTrue("id1 found", ids.contains(id1));
        assertTrue("id2 found", ids.contains(id2));
    }

    @Test
    public void testSelectHuman() {
        SearchResult results = search.select("human", 0, 10);
        assertNotNull("Results found", results);
        assertEquals("2 results", 2, results.getResults().size());
        assertTrue("Human found", results.getResults().stream().anyMatch(r -> r.get("id").equals("homo_sapiens")));
    }

    @Test
    public void testSelectEcoli() {
        QueryResult results = search.select("escherichia", 0, 10);
        assertNotNull("Results found", results);
        assertEquals("2 results", 2, results.getResultCount());
        assertEquals("K12 first", "escherichia_coli_str_k_12_substr_mg1655", results.getResults().get(0).get("id"));
    }

}
