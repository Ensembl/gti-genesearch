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
import org.ensembl.genesearch.test.MongoTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.ensembl.genesearch.utils.QueryHandlerTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class VariantSearchTest extends AbstractESTestCase {

    private static DataTypeInfo geneInfo = DataTypeInfo.fromResource("/datatypes/genes_datatype_info.json");
    private static DataTypeInfo variantInfo = DataTypeInfo.fromResource("/datatypes/mongo_variants_datatype_info.json");

    private static MongoTestServer mongoTestServer;
    static Search search;
    private static Search geneSearch;

    @BeforeClass
    public static void initData() throws IOException {
        // index a sample of JSON
        ESSearch esGeneSearch = new ESSearch(esTestClient.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE, geneInfo);
        mongoTestServer = new MongoTestServer();

        Search mSearch = new MongoSearch(mongoTestServer.getCollection(), variantInfo);
        SearchRegistry provider = new SearchRegistry().registerSearch(SearchType.VARIANTS, mSearch).registerSearch(SearchType.GENES, esGeneSearch);
        search = new VariantSearch(provider);
        geneSearch = new GeneSearch(provider);

        log.info("Reading documents");
        String variantJson = DataUtils.readGzipResource("/variants.json.gz");
        mongoTestServer.indexData(variantJson);
        String geneData = DataUtils.readGzipResource("/rice_genes.json.gz");
        esTestClient.indexTestDocs(geneData, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
    }

    @Test
    public void testQueryAll() {
        QueryResult result = search.query(Collections.emptyList(), QueryOutput.build("[\"chr\",\"annot\"]"),
                Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Checking for result fields", 3, result.getFields().size());
        assertEquals("Checking for correct rows", 5, result.getResults().size());
    }

    @Test
    public void testQuery() {
        QueryResult result = search.query(QueryHandlerTest.build("{\"chr\":\"Chr1\"}"), QueryOutput.build("[\"chr\",\"annot\"]"),
                Collections.emptyList(), 0, 5, Collections.emptyList());
        assertEquals("Checking for result fields", 3, result.getFields().size());
        assertEquals("Checking for correct rows", 5, result.getResults().size());
        assertTrue("", result.getResults().stream().allMatch(r -> r.get("chr").equals("Chr1")));
    }

    @Test
    public void testFetch() {
        SearchResult result = search.fetch(QueryHandlerTest.build("{\"chr\":\"Chr1\"}"), QueryOutput.build("[\"chr\",\"annot\"]"));
        assertEquals("Checking for result fields", 3, result.getFields().size());
        assertEquals("Checking for correct rows", 10, result.getResults().size());
        assertTrue("Check we've only got Chr 1",
                result.getResults().stream().allMatch(r -> r.get("chr").equals("Chr1")));
    }

    @Test
    public void testFetchConsume() {
        AtomicInteger i = new AtomicInteger(0);
        search.fetch(r -> {
            assertEquals("Checking for result fields", 3, r.keySet().size());
            assertEquals("Checking for Chr1", "Chr1", r.get("chr"));
            i.incrementAndGet();
        }, QueryHandlerTest.build("{\"chr\":\"Chr1\"}"), QueryOutput.build("[\"chr\",\"annot\"]"));
        assertEquals("Checking for correct rows", 10, i.get());
    }

    @Test
    public void testQuerySub() {
        QueryResult result = search.query(QueryHandlerTest.build("{\"annot\":{\"ct-list\":{\"ensg\":\"OS01G0100100\"}}}"),
                QueryOutput.build("[\"chr\",\"annot\"]"), Collections.emptyList(), 0, 5, Collections.emptyList());
        //System.out.println(result.getResults());
        assertEquals("Checking for result fields", 3, result.getFields().size());
        assertEquals("Checking for correct rows", 5, result.getResults().size());
        assertTrue("", result.getResults().stream().allMatch(r -> r.get("chr").equals("Chr1")));
    }

    @Test
    public void testQuerySubDub() {
        QueryResult result = search.query(
                QueryHandlerTest.build("{\"annot\":{\"ct-list\":{\"ensg\":\"OS01G0100100\",\"so\":1631}}}"),
                QueryOutput.build("[\"chr\",\"annot\"]"), Collections.emptyList(), 0, 5, Collections.emptyList());
        //System.out.println(result.getResults());
        assertEquals("Checking for result fields", 3, result.getFields().size());
        assertEquals("Checking for correct rows", 5, result.getResults().size());
        assertTrue("", result.getResults().stream().allMatch(r -> r.get("chr").equals("Chr1")));
    }

    @Test
    public void testQueryJoinTo() {
        QueryResult result = geneSearch.query(Collections.emptyList(),
                QueryOutput.build("\"id\",{\"variants\":[\"chr\",\"annot\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        assertEquals("Checking for correct rows", 2, result.getResults().size());
        Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("EPlOSAG00000002326")).findFirst().get();
        assertTrue("ID found", gene.containsKey("id"));
        assertTrue("Variants found", gene.containsKey("variants"));
        List<Map<String, Object>> variants = (List) gene.get("variants");
        assertEquals("Checking for correct rows", 10, variants.size());
        for (String key : new String[]{"_id", "chr", "annot"}) {
            assertTrue(key + " found", variants.stream().allMatch(v -> v.containsKey(key)));
        }
    }

    @Test
    public void testQueryJoinToCount() {
        QueryResult result = geneSearch.query(Collections.emptyList(),
                QueryOutput.build("\"id\",{\"variants\":[\"count\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        //System.out.println(result.getResults());
        assertEquals("Checking for correct rows", 2, result.getResults().size());
        Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("EPlOSAG00000002326")).findFirst().get();
        assertTrue("ID found", gene.containsKey("id"));
        assertTrue("Variants found", gene.containsKey("variants"));
        Map<String, Object> variants = (Map<String, Object>) gene.get("variants");
        assertTrue("Count found", variants.containsKey("count"));
        int count = (int) variants.get("count");
        assertEquals("Count correct", 10, count);
    }

    @Test
    public void testQueryJoinToInner() {
        QueryResult result = geneSearch.query(QueryHandlerTest.build("{\"variants\":{\"inner\":1}}"),
                QueryOutput.build("\"id\",{\"variants\":[\"chr\",\"annot\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        assertEquals("Checking for correct rows", 1, result.getResults().size());
        Map<String, Object> gene = result.getResults().get(0);
        assertTrue("ID found", gene.containsKey("id"));
        assertTrue("Variants found", gene.containsKey("variants"));
        List<Map<String, Object>> variants = (List) gene.get("variants");
        assertEquals("Checking for correct rows", 10, variants.size());
        for (String key : new String[]{"_id", "chr", "annot"}) {
            assertTrue(key + " found", variants.stream().allMatch(v -> v.containsKey(key)));
        }
    }

    @Test
    public void testQueryJoinToInnerQuery() {
        QueryResult result = geneSearch.query(QueryHandlerTest.build("{\"variants\":{\"inner\":1, \"annot\":{\"ct-list\":{\"so\":1631}}}}"),
                QueryOutput.build("\"id\",{\"variants\":[\"chr\",\"annot\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        assertEquals("Checking for correct rows", 1, result.getResults().size());
        Map<String, Object> gene = result.getResults().get(0);
        assertTrue("ID found", gene.containsKey("id"));
        assertTrue("Variants found", gene.containsKey("variants"));
        List<Map<String, Object>> variants = (List) gene.get("variants");
        assertEquals("Checking for correct rows", 10, variants.size());
        for (String key : new String[]{"_id", "chr", "annot"}) {
            assertTrue(key + " found", variants.stream().allMatch(v -> v.containsKey(key)));
        }
    }

    @Test
    public void testQueryJoinToInnerQueryExcl() {
        QueryResult result = geneSearch.query(QueryHandlerTest.build("{\"variants\":{\"inner\":1, \"annot\":{\"ct-list\":{\"so\":16310}}}}"),
                QueryOutput.build("\"id\",{\"variants\":[\"chr\",\"annot\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        assertEquals("Checking for correct rows", 0, result.getResults().size());
    }

    @AfterClass
    public static void tearDownMongo() {
        log.info("Disconnecting Mongo server");
        mongoTestServer.disconnect();
    }

}
