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
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class EVAGenomeFinderTest extends AbstractESTestCase {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(WireMockConfiguration.options().dynamicPort());

    private static ESSearch ensemblGenomeSearch;
    private static EVAGenomeFinder finder;

    @BeforeClass
    public static void initData() throws IOException {
        // index a sample of JSON
        log.info("Reading documents");
        ensemblGenomeSearch = new ESSearch(esTestClient.getClient(), ESSearch.GENOMES_INDEX,
                ESSearch.GENOME_ESTYPE, DataTypeInfo.fromResource("/datatypes/genomes_datatype_info.json"));
        String json = DataUtils.readGzipResource("/eva_genomes.json.gz");
        log.info("Creating test index");
        esTestClient.indexTestDocs(json, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
        finder = new EVAGenomeFinder(new EVAGenomeRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/datatypes/evagenomes_datatype_info.json")), ensemblGenomeSearch);
    }

    @Test
    public void testGrch37() {
        String name = finder.getEVAGenomeName("homo_sapiens");
        Assert.assertEquals("Name found", "hsapiens_grch37", name);
    }

    @Test
    public void testBacteria() {
        // test to see what happens when we don't have a genome in EVA. Null
        // should be returned.
        String name = finder.getEVAGenomeName("brucella_melitensis_gca_000988815");
        Assert.assertTrue("No name found", name == null);
    }

}
