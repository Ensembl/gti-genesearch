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
import org.ensembl.genesearch.info.FieldType;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class ESTranscriptionFactorSearchTest {

    static Logger log = LoggerFactory.getLogger(ESTranscriptionFactorSearchTest.class);

    static ESSearch search;
    static ESTestServer testServer;

    @BeforeClass
    public static void setUp() throws IOException {
        // index a sample of JSON
        testServer = new ESTestServer();
        search = new ESSearch(testServer.getClient(), ESSearch.TRANSCRIPTION_FACTORS_INDEX, ESSearch.TRANSCRIPTION_FACTOR_ESTYPE, DataTypeInfo.fromResource("/datatypes/transcription_factors_datatype_info.json"));
        log.info("Reading documents");
        String json = DataUtils.readGzipResource("/human_transcription_factors.json.gz");
        log.info("Creating test index");
        testServer.indexTestDocs(json, ESSearch.TRANSCRIPTION_FACTORS_INDEX, ESSearch.TRANSCRIPTION_FACTOR_ESTYPE);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void fetchAll() {
        log.info("Fetching all the Transcription factors");
        search.fetch(Collections.emptyList(), QueryOutput.build(Collections.singletonList("_id")));
    }

    @AfterClass
    public static void tearDown() {
        log.info("Disconnecting server");
        testServer.disconnect();
        testServer = null;
    }

}
