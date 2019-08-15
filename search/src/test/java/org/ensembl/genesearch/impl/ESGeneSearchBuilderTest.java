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

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.info.FieldType;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
import org.ensembl.genesearch.utils.DataUtils;
import org.ensembl.genesearch.utils.QueryHandlerTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ESGeneSearchBuilderTest {

    static Logger log = LoggerFactory.getLogger(ESGeneSearchBuilderTest.class);

    @Test
    public void testId() {
        QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                new Query(FieldType.ID, "id", "DDB0231518"));

        Map<String, Object> obj = DataUtils.jsonToMap(builder.toString());
        log.info(obj.toString());
        assertObjCorrect(
                "Object string check",
                "{constant_score={filter={ids={type=[gene],values=[DDB0231518],boost=1.0}},boost=1.0}}",
                obj);
    }

    @Test
    public void testNestedHomology() {
        Query genome = new Query(FieldType.TERM, "genome", "dictyostelium_fasciculatum");
        Query orthology = new Query(FieldType.TERM, "description", "ortholog_one2one");
        Query homology = new Query(FieldType.NESTED, "homologues", genome, orthology);

        QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, homology);

        Map<String, Object> obj = DataUtils.jsonToMap(builder.toString());
        log.info(obj.toString());

        assertTrue("Nested set", obj.containsKey("nested"));
        Map<String, Object> nested = (Map<String, Object>) obj.get("nested");
        assertEquals("Path", "homologues", nested.get("path"));
        assertTrue("Query set", nested.containsKey("query"));
        Map<String, Object> query = (Map<String, Object>) nested.get("query");
        assertTrue("Bool set", query.containsKey("bool"));

        assertObjCorrect("Object string check",
                "{nested={query={bool={must=[{constant_score={filter={term={homologues.genome=" +
                        "{value=dictyostelium_fasciculatum,boost=1.0}}},boost=1.0}},{constant_score={filter=" +
                        "{term={homologues.description={value=ortholog_one2one,boost=1.0}}},boost=1.0}}]," +
                        "adjust_pure_negative=true,boost=1.0}},path=homologues,ignore_unmapped=false," +
                        "score_mode=avg,boost=1.0}}",
                obj);
    }

    @Test
    public void testNestedTranslationId() {
        Query idQuery = new Query(FieldType.TERM, "id", "DDB0231518");
        Query translationQuery = new Query(FieldType.NESTED, "translations", idQuery);
        Query geneQuery = new Query(FieldType.NESTED, "transcripts", translationQuery);
        QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, geneQuery);
        Map<String, Object> obj = DataUtils.jsonToMap(builder.toString());
        log.info(obj.toString());
        assertTrue("Nested set", obj.containsKey("nested"));

        assertObjCorrect("Object string check",
                "{nested={query={nested={query={constant_score={filter={term={transcripts.translations.id=" +
                        "{value=DDB0231518,boost=1.0}}},boost=1.0}},path=transcripts.translations," +
                        "ignore_unmapped=false,score_mode=avg,boost=1.0}},path=transcripts,ignore_unmapped=false," +
                        "score_mode=avg,boost=1.0}}",
                obj);

    }

    @Test
    public void testSimpleFacet() {
        AbstractAggregationBuilder buildAggregation = ESSearchBuilder.buildAggregation("GO", 10);
        assertEquals("Class check", TermsAggregationBuilder.class, buildAggregation.getClass());
    }

    @Test
    public void testNestedFacet() {
        AbstractAggregationBuilder buildAggregation = ESSearchBuilder.buildAggregation("homologues.genome", 10);
        assertEquals("Class check", NestedAggregationBuilder.class, buildAggregation.getClass());
    }

    @Test
    public void testDoubleNestedFacet() {
        AbstractAggregationBuilder buildAggregation = ESSearchBuilder.buildAggregation("homologues.genome.banana", 10);
        assertEquals("Class check", NestedAggregationBuilder.class, buildAggregation.getClass());
    }

    @Test
    public void testNumEq() {
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":123}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertObjCorrect("Simple number check", "{constant_score={filter={term={num={value=123,boost=1.0}}},boost=1.0}}", obj);
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":-123}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertObjCorrect("Negative number check", "{constant_score={filter={term={num={value=-123,boost=1.0}}},boost=1.0}}", obj);
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":123.456}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertObjCorrect("Negative number check", "{constant_score={filter={term={num={value=123.456,boost=1.0}}},boost=1.0}}", obj);
        }
    }

    @Test
    public void testNumGt() {
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\">123\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            log.info(obj.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("false"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\">-123\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("-123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("false"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\">123.456\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("123.456"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("false"));
        }
    }

    @Test
    public void testNumGte() {
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\">=123\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            log.info(obj.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("true"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\">=-123\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("-123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("true"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\">=123.456\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("123.456"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("true"));
        }
    }

    @Test
    public void testNumLt() {
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"<123\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            log.info(obj.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.to").contains("123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.from").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("false"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"<-123\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.to").contains("-123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.from").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("false"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"<123.456\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.to").contains("123.456"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.from").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("false"));
        }
    }

    @Test
    public void testNumLte() {
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"<=123\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            log.info(obj.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.to").contains("123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.from").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("true"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"<=-123\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.to").contains("-123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.from").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("true"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"<=123.456\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.to").contains("123.456"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.from").isEmpty());
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("true"));
        }
    }

    @Test
    public void testNumRange() {
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"123-789\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            log.info(obj.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").contains("789"));
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("true"));
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("true"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"-123--789\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("-123"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").contains("-789"));
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("true"));
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("true"));
        }
        {
            QueryBuilder q = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"num\":\"123.456-789.987\"}").toArray(new Query[]{}));
            Map<String, Object> obj = DataUtils.jsonToMap(q.toString());
            assertTrue("From correct", DataUtils.getObjValsForKey(obj, "range.num.from").contains("123.456"));
            assertTrue("To not set", DataUtils.getObjValsForKey(obj, "range.num.to").contains("789.987"));
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_upper").contains("true"));
            assertTrue("From lower not included",
                    DataUtils.getObjValsForKey(obj, "range.num.include_lower").contains("true"));
        }
    }

    @Test
    public void testLocation() {
        QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                QueryHandlerTest.build("{\"location\":\"DDB0231518:1-100\"}").toArray(new Query[]{}));
        log.info(builder.toString());
        Map<String, Object> obj = DataUtils.jsonToMap(builder.toString());
        assertTrue("Name correct",
                DataUtils.getObjValsForKey(obj, "bool.must.term.seq_region_name.value").contains("DDB0231518"));

        assertTrue("Start from correct", DataUtils.getObjValsForKey(obj, "bool.must.range.start.from").contains("1"));
        assertTrue("Start to correct", DataUtils.getObjValsForKey(obj, "bool.must.range.start.to").contains("100"));
        assertTrue("Start include_lower correct",
                DataUtils.getObjValsForKey(obj, "bool.must.range.start.include_lower").contains("true"));
        assertTrue("Start include_lower correct",
                DataUtils.getObjValsForKey(obj, "bool.must.range.start.include_upper").contains("true"));

        assertTrue("End from correct", DataUtils.getObjValsForKey(obj, "bool.must.range.end.from").contains("1"));
        assertTrue("End to correct", DataUtils.getObjValsForKey(obj, "bool.must.range.end.to").contains("100"));
        assertTrue("End include_upper correct",
                DataUtils.getObjValsForKey(obj, "bool.must.range.end.include_upper").contains("true"));
        assertTrue("End include_lower correct",
                DataUtils.getObjValsForKey(obj, "bool.must.range.end.include_lower").contains("true"));

    }

    @Test
    public void testLocations() {
        QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, QueryHandlerTest
                .build("{\"location\":[\"DDB0231518:1-100\",\"DDB0231518:101-200\"]}").toArray(new Query[]{}));
        Map<String, Object> obj = DataUtils.jsonToMap(builder.toString());
        Map<String, Object> bool = (Map<String, Object>) obj.get("bool");
        List<Map<String, Object>> should = (List<Map<String, Object>>) bool.get("should");
        {
            Map<String, Object> loc = should.get(0);
            assertTrue("Name correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.term.seq_region_name.value").contains("DDB0231518"));

            assertTrue("Start from correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.start.from").contains("1"));
            assertTrue("Start to correct", DataUtils.getObjValsForKey(loc, "bool.must.range.start.to").contains("100"));
            assertTrue("Start include_lower correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.start.include_lower").contains("true"));
            assertTrue("Start include_upper correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.start.include_upper").contains("true"));

            assertTrue("End from correct", DataUtils.getObjValsForKey(loc, "bool.must.range.end.from").contains("1"));
            assertTrue("End to correct", DataUtils.getObjValsForKey(loc, "bool.must.range.end.to").contains("100"));
            assertTrue("End include_upper correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.end.include_upper").contains("true"));
            assertTrue("End include_lower correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.end.include_lower").contains("true"));
        }
        {
            Map<String, Object> loc = should.get(1);
            assertTrue("Name correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.term.seq_region_name.value").contains("DDB0231518"));

            assertTrue("Start from correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.start.from").contains("101"));
            assertTrue("Start to correct", DataUtils.getObjValsForKey(loc, "bool.must.range.start.to").contains("200"));
            assertTrue("Start include_lower correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.start.include_lower").contains("true"));
            assertTrue("Start include_upper correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.start.include_upper").contains("true"));

            assertTrue("End from correct", DataUtils.getObjValsForKey(loc, "bool.must.range.end.from").contains("101"));
            assertTrue("End to correct", DataUtils.getObjValsForKey(loc, "bool.must.range.end.to").contains("200"));
            assertTrue("End include_upper correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.end.include_upper").contains("true"));
            assertTrue("End include_lower correct",
                    DataUtils.getObjValsForKey(loc, "bool.must.range.end.include_lower").contains("true"));
        }
    }

    @Test
    public void testLargeTerms() throws IOException {
        QueryHandler handler = new DefaultQueryHandler();
        String json = DataUtils.readGzipResource("/q08_human_swissprot_full.json.gz");
        List<Query> qs = handler.parseQuery(json);
        QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE, qs.get(0));
        Map<String, Object> obj = DataUtils.jsonToMap(builder.toString());
        assertTrue("Constant_score set", obj.containsKey("constant_score"));
        Map<String, Object> constant = (Map<String, Object>) obj.get("constant_score");
        assertTrue("filter set", constant.containsKey("filter"));
        Map<String, Object> filter = (Map<String, Object>) constant.get("filter");
        assertTrue("Terms set", filter.containsKey("terms"));
        Map<String, Object> terms = (Map<String, Object>) filter.get("terms");
        assertTrue("Uniprot_SWISSPROT set", terms.containsKey("Uniprot_SWISSPROT"));
        List<String> uniprot = (List<String>) (terms.get("Uniprot_SWISSPROT"));
        assertEquals("Uniprot_SWISSPROT size", 18920, uniprot.size());
    }

    @Test
    public void testNot() {
        {
            QueryBuilder builder = ESSearchBuilder.buildQuery(ESSearch.GENE_ESTYPE,
                    QueryHandlerTest.build("{\"!fruit\":\"banana\"}").toArray(new Query[]{}));
            assertTrue("End include_lower correct", DataUtils.getObjValsForKey(DataUtils.jsonToMap(builder.toString()),
                    "bool.must_not.constant_score.filter.term.fruit.value").contains("banana"));
        }
    }

    private static void assertObjCorrect(String message, String expected, Object obj) {
        String actual = obj.toString().replaceAll("\\s+", "");
        expected = expected.replaceAll("\\s+", "");
        assertEquals(message, expected, actual);
    }

}
