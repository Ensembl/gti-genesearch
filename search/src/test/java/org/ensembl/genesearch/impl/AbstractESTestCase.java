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
    public static void setUp() throws Exception {
        // index a sample of JSON
        esTestClient = new ESTestClient();
        // only create index when using docker, local test server should be up and set up already
        esTestClient.createIndex(ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        esTestClient.createIndex(ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
        esTestClient.createIndex(ESSearch.VARIANTS_INDEX, ESSearch.VARIANT_ESTYPE);
        esTestClient.createIndex(ESSearch.PROBES_INDEX, ESSearch.PROBE_ESTYPE);
        esTestClient.createIndex(ESSearch.PROBESETS_INDEX, ESSearch.PROBESET_ESTYPE);
        esTestClient.createIndex(ESSearch.MOTIFS_INDEX, ESSearch.MOTIF_ESTYPE);
        esTestClient.createIndex(ESSearch.REGULATORY_FEATURES_INDEX, ESSearch.REGULATORY_FEATURE_ESTYPE);
        esTestClient.createIndex(ESSearch.EXTERNAL_FEATURES_INDEX, ESSearch.EXTERNAL_FEATURE_ESTYPE);
        esTestClient.createIndex(ESSearch.MIRNAS_INDEX, ESSearch.MIRNA_ESTYPE);
        esTestClient.createIndex(ESSearch.PEAKS_INDEX, ESSearch.PEAK_ESTYPE);
        esTestClient.createIndex(ESSearch.TRANSCRIPTION_FACTORS_INDEX, ESSearch.TRANSCRIPTION_FACTOR_ESTYPE);
    }

    @AfterClass
    public static void tearDown(){
        log.info("Disconnecting server");
        esTestClient.disconnect();
    }
}