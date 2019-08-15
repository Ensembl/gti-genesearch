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
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class ESProbeSearchTest extends AbstractESTestCase{

    static ESSearch search;

    @BeforeClass
    public static void initData() throws IOException {
        // index a sample of JSON
        search = new ESSearch(esTestClient.getClient(), ESSearch.PROBES_INDEX, ESSearch.PROBE_ESTYPE, DataTypeInfo.fromResource("/datatypes/probes_datatype_info.json"));
        log.info("Reading documents");
        String json = DataUtils.readGzipResource("/human_probes.json.gz");
        log.info("Creating test index");
        esTestClient.indexTestDocs(json, ESSearch.PROBES_INDEX, ESSearch.PROBE_ESTYPE);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fetchAll() {
        log.info("Fetching all the probes");
        search.fetch(Collections.emptyList(), QueryOutput.build(Collections.singletonList("_id")));
    }

}
