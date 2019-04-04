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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;

public class EnsemblVariantSearchTest {

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(WireMockConfiguration.options().dynamicPort());

    static Logger log = LoggerFactory.getLogger(ESGenomeSearchTest.class);
    static EnsemblVariantSearch search;

    @BeforeClass
    public static void setUp() throws IOException {

        String url = wireMockRule.url(StringUtils.EMPTY);        

        search = new EnsemblVariantSearch(url,
                DataTypeInfo.fromResource("/datatypes/evavariants_datatype_info.json"));

    }

    @Test
    public void testQueryByRange() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EnsemblVariantSearch.LOCATION_FIELD, "7:140601681-140605622"),
                        new Query(FieldType.TERM, EnsemblVariantSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"id\",\"consequence_type\"]"), Collections.emptyList(), 0, 500, Collections.emptyList());
        Assert.assertEquals("486 results found", 486, res.getResults().size());
    }

    @Test
    public void testQueryByRangeLimit() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EnsemblVariantSearch.LOCATION_FIELD, "7:140601681-140605622"),
                        new Query(FieldType.TERM, EnsemblVariantSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"id\",\"consequence_type\"]"), Collections.emptyList(), 0, 100, Collections.emptyList());
        Assert.assertEquals("100 results found", 100, res.getResults().size());
    }
    
    @Test
    public void testQueryByRangeOffset() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EnsemblVariantSearch.LOCATION_FIELD, "7:140601681-140605622"),
                        new Query(FieldType.TERM, EnsemblVariantSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"id\",\"consequence_type\"]"), Collections.emptyList(), 400, 100, Collections.emptyList());
        Assert.assertEquals("86 results found", 86, res.getResults().size());
    }
    
    @Test
    public void testQueryByRangeWithPostFilter() {
        QueryResult res = search.query(
                Arrays.asList(new Query(FieldType.TERM, EnsemblVariantSearch.LOCATION_FIELD, "7:140601681-140605622"),
                        new Query(FieldType.TERM, "consequence_type", "missense_variant"),
                        new Query(FieldType.TERM, EnsemblVariantSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"id\",\"consequence_type\"]"), Collections.emptyList(), 0, 500, Collections.emptyList());
        System.out.println(res.getResults());
        Assert.assertEquals("102 results found", 102, res.getResults().size());
        Assert.assertTrue(res.getResults().stream().anyMatch(v -> {return String.valueOf(v.get("consequence_type")).matches("missense_variant");}));
        Assert.assertFalse(res.getResults().stream().anyMatch(v -> {return String.valueOf(v.get("consequence_type")).matches("intron_variant");}));
    }

    @Test
    public void testFetchByRange() {
        SearchResult res = search.fetch(
                Arrays.asList(new Query(FieldType.TERM, EnsemblVariantSearch.LOCATION_FIELD, "7:140601681-140605622"),
                        new Query(FieldType.TERM, EnsemblVariantSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"id\",\"consequence_type\"]"));
        Assert.assertEquals("486 results found", 486, res.getResults().size());
    }
    
    @Test
    public void testFetchByRangeWithPostFilter() {
        SearchResult res = search.fetch(
                Arrays.asList(new Query(FieldType.TERM, EnsemblVariantSearch.LOCATION_FIELD, "7:140601681-140605622"),
                        new Query(FieldType.TERM, "consequence_type", "missense_variant"),
                        new Query(FieldType.TERM, EnsemblVariantSearch.GENOME_FIELD, "homo_sapiens")),
                QueryOutput.build("[\"id\",\"consequence_type\"]"));
        Assert.assertEquals("102 results found", 102, res.getResults().size());
        Assert.assertTrue(res.getResults().stream().anyMatch(v -> {return String.valueOf(v.get("consequence_type")).matches("missense_variant");}));
        Assert.assertFalse(res.getResults().stream().anyMatch(v -> {return String.valueOf(v.get("consequence_type")).matches("intron_variant");}));
    }
}
