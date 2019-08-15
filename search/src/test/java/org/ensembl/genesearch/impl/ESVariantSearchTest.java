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

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.utils.QueryHandlerTest;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;
import org.ensembl.genesearch.utils.DataUtils;
import org.ensembl.genesearch.test.ESTestServer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class ESVariantSearchTest {

    static Logger log = LoggerFactory.getLogger(ESVariantSearchTest.class);

    static ESTestServer testServer;
    static ESSearch search;

    @BeforeClass
    public static void setUp() throws IOException {
        // index a sample of JSON
        testServer = new ESTestServer();
        log.info("Reading documents");
        search = new ESSearch(testServer.getClient(), ESSearch.VARIANTS_INDEX, ESSearch.VARIANT_ESTYPE,
                DataTypeInfo.fromResource("/datatypes/es_variants_datatype_info.json"));
        String json = DataUtils.readGzipResource("/es_variants.json.gz");
        log.info("Creating test index");
        testServer.indexTestDocs(json, ESSearch.VARIANTS_INDEX, ESSearch.VARIANT_ESTYPE);
    }

    @Test
    public void fetchAll() {
        log.info("Fetching all variants");
        try {
            search.fetch(Collections.emptyList(), QueryOutput.build(Collections.singletonList("_id")));
            fail("Illegal operation succeeded");
        } catch (UnsupportedOperationException e) {
            // OK
        }
    }

    @Test
    public void testQueryById() {
        QueryResult res = search.query(Collections.singletonList(new Query(FieldType.TERM, "_id", "rs192")),
                QueryOutput.build("[\"_id\"]"), Collections.emptyList(), 0, 10, Collections.emptyList());
        Assert.assertEquals("1 result found", 1, res.getResults().size());
    }

    @Test
    public void testQueryByRange() {
        QueryResult res = search.query(
                QueryHandlerTest.build("{\"locations\":{\"location\":\"7:11547118-24405439\"}}"),
                QueryOutput.build("[\"_id\"]"), Collections.emptyList(), 0, 50, Collections.emptyList());
        Assert.assertEquals("59 results found", 59, res.getResultCount());
    }

    @AfterClass
    public static void tearDown() {
        log.info("Disconnecting server");
        testServer.disconnect();
    }

}
