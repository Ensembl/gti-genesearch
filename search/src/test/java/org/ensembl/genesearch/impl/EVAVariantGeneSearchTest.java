package org.ensembl.genesearch.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;
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

public class EVAVariantGeneSearchTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(WireMockConfiguration.options().dynamicPort());

    static Logger log = LoggerFactory.getLogger(ESGenomeSearchTest.class);
    static GeneSearch geneSearch;

    @BeforeClass
    public static void setUp() throws IOException {

        ESTestServer testServer = new ESTestServer();
        // index a sample of JSON for use in search genomes
        log.info("Reading documents");
        {
            String json = DataUtils.readGzipResource("/eva_genomes.json.gz");
            log.info("Creating test index for genomes");
            testServer.indexTestDocs(json, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
        }
        {
            String json = DataUtils.readGzipResource("/eva_genes.json.gz");
            log.info("Creating test index for genomes");
            testServer.indexTestDocs(json, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        }
        ESSearch ensemblGenomeSearch = new ESSearch(testServer.getClient(), ESSearch.GENOMES_INDEX,
                ESSearch.GENOME_ESTYPE, DataTypeInfo.fromResource("/genomes_datatype_info.json"));

        ESSearch ensemblGeneSearch = new ESSearch(testServer.getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE,
                DataTypeInfo.fromResource("/genes_datatype_info.json"));

        // build a finder using the test ES server and a wiremock REST
        // implementation
        EVAGenomeFinder finder = new EVAGenomeFinder(new EVAGenomeRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/evagenomes_datatype_info.json")), ensemblGenomeSearch);

        EVAVariantRestSearch variantSearch = new EVAVariantRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/evavariants_datatype_info.json"), finder);

        SearchRegistry provider = new SearchRegistry();
        provider.registerSearch(SearchType.GENES, ensemblGeneSearch);
        provider.registerSearch(SearchType.GENOMES, ensemblGenomeSearch);
        provider.registerSearch(SearchType.VARIANTS, variantSearch);
        geneSearch = new GeneSearch(provider);
    }

    @Test
    public void testQueryJoinToSingle() {
        QueryResult result = geneSearch.query(Arrays.asList(new Query(FieldType.TERM, "id", "ENSG00000270921")),
                QueryOutput.build("\"id\",{\"variants\":[\"ids\",\"chromosome\",\"start\",\"reference\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        System.out.println(result.getResults());
        Assert.assertEquals("Checking for correct rows", 1, result.getResults().size());
        Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("ENSG00000270921"))
                .findFirst().get();
        Assert.assertTrue("ID found", gene.containsKey("id"));
        Assert.assertTrue("Variants found", gene.containsKey("variants"));
        List<Map<String, Object>> variants = (List) gene.get("variants");
        Assert.assertEquals("Checking for correct rows", 30, variants.size());
        for (String key : new String[] { "ids", "chromosome", "start", "reference" }) {
            Assert.assertTrue(key + " found", variants.stream().allMatch(v -> v.containsKey(key)));
        }
        for (String key : new String[] { "alternate" }) {
            Assert.assertFalse(key + " not found", variants.stream().allMatch(v -> v.containsKey(key)));
        }
    }

    @Test
    public void testQueryJoinToSingleClientFilterQuery() {
        QueryResult result = geneSearch.query(Arrays.asList(new Query(FieldType.TERM, "id", "ENSG00000270921"), new Query(FieldType.NESTED, "variants", new Query(FieldType.TERM, "reference", "G"))),
                QueryOutput.build("\"id\",{\"variants\":[\"ids\",\"chromosome\",\"start\",\"reference\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        Assert.assertEquals("Checking for correct rows", 1, result.getResults().size());
        Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("ENSG00000270921"))
                .findFirst().get();
        Assert.assertTrue("ID found", gene.containsKey("id"));
        Assert.assertTrue("Variants found", gene.containsKey("variants"));
        List<Map<String, Object>> variants = (List) gene.get("variants");
        Assert.assertEquals("Checking for correct rows", 12, variants.size());
        System.out.println(variants);
        Assert.assertTrue("Only reference G not found", variants.stream().allMatch(v -> "G".equals(v.get("reference"))));
    }
    
    @Test
    public void testQueryJoinToSingleServerFilterQuery() {
        QueryResult result = geneSearch.query(Arrays.asList(new Query(FieldType.TERM, "id", "ENSG00000270921"), new Query(FieldType.NESTED, "variants", new Query(FieldType.TERM, "annot-ct", "SO:0001627"))),
                QueryOutput.build("\"id\",{\"variants\":[\"ids\",\"chromosome\",\"start\",\"annotation.consequenceTypes.soTerms\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        Assert.assertEquals("Checking for correct rows", 1, result.getResults().size());
        Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("ENSG00000270921"))
                .findFirst().get();
        Assert.assertTrue("ID found", gene.containsKey("id"));
        Assert.assertTrue("Variants found", gene.containsKey("variants"));
        List<Map<String, Object>> variants = (List) gene.get("variants");
        Assert.assertEquals("Checking for correct rows", 26, variants.size());
        System.out.println(variants);
        Assert.assertTrue("Only reference G not found", variants.stream().allMatch(v -> DataUtils.getObjValsForKey(v, "annotation.consequenceTypes.soTerms.soAccession").contains("SO:0001627")));
    }
    
    @Test
    public void testQueryJoinToMultiple() {
        QueryResult result = geneSearch.query(Collections.emptyList(),
                QueryOutput.build("\"id\",{\"variants\":[\"ids\"]}"), Collections.emptyList(), 0, 10,
                Collections.emptyList());
        Assert.assertEquals("Checking for correct rows", 2, result.getResults().size());
        {
            Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("ENSG00000270921"))
                    .findFirst().get();
            Assert.assertTrue("ID found", gene.containsKey("id"));
            Assert.assertTrue("Variants found", gene.containsKey("variants"));
        }
        {
            Map<String, Object> gene = result.getResults().stream().filter(g -> g.get("id").equals("ENSG00000080947"))
                    .findFirst().get();
            Assert.assertTrue("ID found", gene.containsKey("id"));
            Assert.assertFalse("Variants not found", gene.containsKey("variants"));
        }
    }

}
