package org.ensembl.genesearch.impl;

import com.carrotsearch.randomizedtesting.annotations.ThreadLeakScope;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;
import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.genesearch.utils.DataUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
@ThreadLeakScope(ThreadLeakScope.Scope.NONE)
public class EVAVariantRestSearchTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(WireMockConfiguration.options().dynamicPort());

    static Logger log = LoggerFactory.getLogger(ESGenomeSearchTest.class);
    static EVAVariantRestSearch search;

    @BeforeClass
    public static void setUp() throws IOException {

        ESTestServer testServer = new ESTestServer();
        // index a sample of JSON for use in search genomes
        log.info("Reading documents");
        String json = DataUtils.readGzipResource("/eva_genomes.json.gz");
        log.info("Creating test index for genomes");
        testServer.indexTestDocs(json, ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
        ESSearch ensemblGenomeSearch = new ESSearch(testServer.getClient(), ESSearch.GENOMES_INDEX,
                ESSearch.GENOME_ESTYPE, DataTypeInfo.fromResource("/datatypes/genomes_datatype_info.json"));

        // build a finder using the test ES server and a wiremock REST
        // implementation
        EVAGenomeFinder finder = new EVAGenomeFinder(new EVAGenomeRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/datatypes/evagenomes_datatype_info.json")), ensemblGenomeSearch);

        search = new EVAVariantRestSearch(wireMockRule.url(StringUtils.EMPTY),
                DataTypeInfo.fromResource("/datatypes/evavariants_datatype_info.json"), finder);

    }

    @Test
    public void testQueryById() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EVAVariantRestSearch.ID_FIELD, "rs666"),
                        new Query(FieldType.TERM, EVAVariantRestSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"ids\"]"), Collections.emptyList(), 0, 10, Collections.emptyList());
        Assert.assertEquals("1 result found", 1, res.getResults().size());
    }

    @Test
    public void testQueryByRange() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EVAVariantRestSearch.LOCATION_FIELD, "11:128446-129000"),
                        new Query(FieldType.TERM, EVAVariantRestSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"ids\"]"), Collections.emptyList(), 0, 50, Collections.emptyList());
        Assert.assertEquals("34 results found", 34, res.getResults().size());
    }

    @Test
    public void testQueryByRangeWithRequestFilter() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EVAVariantRestSearch.LOCATION_FIELD, "11:128446-129000"),
                        new Query(FieldType.TERM, "annot-ct", "SO:0001782"),
                        new Query(FieldType.TERM, EVAVariantRestSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"ids\"]"), Collections.emptyList(), 0, 50, Collections.emptyList());
        Assert.assertEquals("4 results found", 4, res.getResults().size());
    }

    @Test
    public void testQueryByRangeWithPostFilter() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EVAVariantRestSearch.LOCATION_FIELD, "11:128446-129000"),
                        new Query(FieldType.TERM, "alternate", "A"),
                        new Query(FieldType.TERM, EVAVariantRestSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"ids\"]"), Collections.emptyList(), 0, 50, Collections.emptyList());
        Assert.assertEquals("11 results found", 11, res.getResults().size());
    }

    @Test
    public void testFetchByRange() {
        {
            search.setBatchSize(10);
            SearchResult res = search.fetch(
                    Arrays.asList(new Query(FieldType.TERM, EVAVariantRestSearch.LOCATION_FIELD, "11:128446-129000"),
                            new Query(FieldType.TERM, EVAVariantRestSearch.GENOME_FIELD, "homo_sapiens")),
                    QueryOutput.build("[\"ids\"]"));
            Assert.assertEquals("34 results found", 34, res.getResults().size());
        }
        {
            search.setBatchSize(1000);
            SearchResult res = search.fetch(
                    Arrays.asList(new Query(FieldType.TERM, EVAVariantRestSearch.LOCATION_FIELD, "11:128446-129000"),
                            new Query(FieldType.TERM, EVAVariantRestSearch.GENOME_FIELD, "homo_sapiens")),
                    QueryOutput.build("[\"ids\"]"));
            Assert.assertEquals("34 results found", 34, res.getResults().size());
        }
    }

    @Test
    public void testQueryByIdWithOutput() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EVAVariantRestSearch.ID_FIELD, "rs666"),
                        new Query(FieldType.TERM, EVAVariantRestSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput
                        .build("[\"ids\",\"alternate\",\"hgvs\",{\"annotation\":[\"consequenceTypes\",{\"xrefs\":[\"src\"]}]}]"),
                Collections.emptyList(), 0, 10, Collections.emptyList());
        Assert.assertEquals("1 result found", 1, res.getResults().size());
        Map<String, Object> v = res.getResults().get(0);
        Assert.assertFalse("ids found", DataUtils.getObjValsForKey(v, "ids").isEmpty());
        Assert.assertFalse("alternate found", DataUtils.getObjValsForKey(v, "alternate").isEmpty());
        Assert.assertTrue("reference not found", DataUtils.getObjValsForKey(v, "reference").isEmpty());
        Assert.assertFalse("hgvs.genomic found", DataUtils.getObjValsForKey(v, "hgvs.genomic").isEmpty());
        Assert.assertFalse("annotation.consequenceTypes found",
                DataUtils.getObjValsForKey(v, "annotation.consequenceTypes").isEmpty());
        Assert.assertTrue("annotation.chromosome found",
                DataUtils.getObjValsForKey(v, "annotation.chromosome").isEmpty());
        Assert.assertFalse("annotation.xrefs.src found",
                DataUtils.getObjValsForKey(v, "annotation.xrefs.src").isEmpty());
        Assert.assertTrue("annotation.xrefs.id not found",
                DataUtils.getObjValsForKey(v, "annotation.xrefs.id").isEmpty());
    }

}
