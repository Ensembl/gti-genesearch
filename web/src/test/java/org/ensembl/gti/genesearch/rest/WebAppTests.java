package org.ensembl.gti.genesearch.rest;

import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.test.ESTestClient;
import org.ensembl.genesearch.utils.DataUtils;
import org.ensembl.gti.genesearch.services.EndpointSearchProvider;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

abstract public class WebAppTests {


    @Value("${local.server.port}")
    int port;

    protected String GENES_FETCH = "genes/fetch";
    protected String GENES_QUERY = "genes/query";
    protected String GENOMES_FETCH = "genomes/fetch";
    protected String GENOMES_QUERY = "genomes/query";
    protected String GENOMES_SELECT = "genomes/select";
    protected String TRANSCRIPTS_FETCH = "transcripts/fetch";
    protected String TRANSCRIPTS_QUERY = "transcripts/query";
    protected String VARIANTS_FETCH = "variants/fetch";
    protected String VARIANTS_QUERY = "variants/query";
    protected String INFO = "genes/info";
    protected static Logger log = LoggerFactory.getLogger(EndpointTests.class);
    protected static ESTestClient esTestClient;

    static ESSearch geneSearch;
    static ESSearch genomeSearch;


    @Autowired
    EndpointSearchProvider provider;

    protected String getServiceUrl(String extension) {
        String base_path = "http://localhost:" + port;
        if (extension == null)
            return base_path;
        else
            return base_path + "/api/" + extension;
    }

    public static void initSetUp() throws IOException {
        // create our ES test server once only
        log.info("Setting up ");
        esTestClient = new ESTestClient();
        log.info("Reading documents");
        String geneJson = DataUtils.readGzipResource("/nanoarchaeum_equitans_kin4_m_genes.json.gz");
        String genomeJson = DataUtils.readGzipResource("/genomes.json.gz");
        log.info("Creating test index");
        esTestClient.indexTestDocs(geneJson, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        esTestClient.indexTestDocs(genomeJson, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);

    }

    @Before
    public void injectSearch() {
        // ensure we always use our test instances
        log.info("Inject search " + esTestClient.getClient());
        provider.setESClient(esTestClient.getClient());
    }

}
