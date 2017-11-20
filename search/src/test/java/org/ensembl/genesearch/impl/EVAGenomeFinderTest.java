package org.ensembl.genesearch.impl;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

public class EVAGenomeFinderTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(WireMockConfiguration.options().dynamicPort());

    static Logger log = LoggerFactory.getLogger(ESGenomeSearchTest.class);

    static ESTestServer testServer = new ESTestServer();
    static ESSearch ensemblGenomeSearch = new ESSearch(testServer.getClient(), ESSearch.GENOMES_INDEX,
            ESSearch.GENOME_ESTYPE, DataTypeInfo.fromResource("/genomes_datatype_info.json"));
    static EVAGenomeFinder finder;

    @BeforeClass
    public static void setUp() throws IOException {
        // index a sample of JSON
        log.info("Reading documents");
        String json = DataUtils.readGzipResource("/eva_genomes.json.gz");
        log.info("Creating test index");
        testServer.indexTestDocs(json, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
        finder = new EVAGenomeFinder(new EVAGenomeRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/evagenomes_datatype_info.json")), ensemblGenomeSearch);
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
