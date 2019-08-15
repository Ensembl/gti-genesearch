package org.ensembl.genesearch.impl;

import org.ensembl.genesearch.test.ESTestClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractESTestCase {
    static Logger log = LoggerFactory.getLogger(ESGeneSearchTest.class);
    static ESTestClient esTestClient;

    @BeforeClass
    public static void setUp(){
        // index a sample of JSON
        esTestClient = new ESTestClient();
    }

    @AfterClass
    public static void tearDown(){
        log.info("Disconnecting server");
        esTestClient.disconnect();
    }
}