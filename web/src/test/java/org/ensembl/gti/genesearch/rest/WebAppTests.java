package org.ensembl.gti.genesearch.rest;

import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.test.ESTestClient;
import org.ensembl.genesearch.utils.DataUtils;
import org.ensembl.gti.genesearch.services.Application;
import org.ensembl.gti.genesearch.services.EndpointSearchProvider;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.junit.Assert.assertNotNull;

/**
 * @author mchakiachvili
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@SpringBootApplication
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public class WebAppTests {


    @Value("${local.server.port}")
    int port;

    String GENES_FETCH = "genes/fetch";
    String GENES_QUERY = "genes/query";
    String GENOMES_FETCH = "genomes/fetch";
    String GENOMES_QUERY = "genomes/query";
    String GENOMES_SELECT = "genomes/select";
    String TRANSCRIPTS_FETCH = "transcripts/fetch";
    String TRANSCRIPTS_QUERY = "transcripts/query";
    String VARIANTS_FETCH = "variants/fetch";
    String VARIANTS_QUERY = "variants/query";
    String INFO = "genes/info";

    static Logger log = LoggerFactory.getLogger(EndpointTests.class);
    static ESTestClient esTestClient;

    @Autowired
    EndpointSearchProvider provider;

    String getServiceUrl(String extension) {
        String base_path = "http://localhost:" + port;
        if (extension == null)
            return base_path;
        else
            return base_path + "/api/" + extension;
    }

    @BeforeClass
    public static void initSetUp() throws Exception {
        // create our ES test server once only
        esTestClient = new ESTestClient();

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

        String geneJson = DataUtils.readGzipResource("/nanoarchaeum_equitans_kin4_m_genes.json.gz");
        String genomeJson = DataUtils.readGzipResource("/genomes.json.gz");
        esTestClient.indexTestDocs(geneJson, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        esTestClient.indexTestDocs(genomeJson, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);

    }

    @Before
    public void injectSearch() {
        // ensure we always use our test instances
        log.info("Inject search " + esTestClient.getClient() + " for provider " + provider);
        provider.setESClient(esTestClient.getClient());
    }

    @Test
    public void testInit() {
        log.info("Test Init Cluster ");
        assertNotNull(esTestClient);
    }
}
