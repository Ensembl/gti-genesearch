package org.ensembl.genesearch.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.info.FieldType;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for {@link QueryUtils}
 * 
 * @author dstaines
 *
 */
public class QueryUtilsTest {

    @Test
    public void testFilterFieldsSimple() {
        Map<String, Object> o = new HashMap<>();
        o.put("fruit", "apple");
        o.put("colour", "red");
        o.put("ripeness", "ripe");
        QueryOutput output = new QueryOutput("fruit", "colour");
        QueryUtils.filterFields(o, output);
        Assert.assertTrue("Fruit found", o.containsKey("fruit"));
        Assert.assertTrue("Colour found", o.containsKey("colour"));
        Assert.assertFalse("Ripeness found", o.containsKey("ripeness"));
    }
    
    @Test
    public void testFilterFieldsWild() {
        Map<String, Object> o = new HashMap<>();
        o.put("fruit", "apple");
        o.put("colour", "red");
        o.put("ripeness", "ripe");
        QueryOutput output = new QueryOutput("*");
        QueryUtils.filterFields(o, output);
        Assert.assertTrue("Fruit found", o.containsKey("fruit"));
        Assert.assertTrue("Colour found", o.containsKey("colour"));
        Assert.assertTrue("Ripeness found", o.containsKey("ripeness"));
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testFilterFieldsNested() throws JsonParseException, JsonMappingException, IOException {
        {
            Map<String, Object> o = new ObjectMapper().readValue("{\"A\":\"str\", \"B\":[1,2,3]}", Map.class);
            QueryOutput output = new QueryOutput("A");
            QueryUtils.filterFields(o, output);
            Assert.assertTrue("A found", o.containsKey("A"));
            Assert.assertFalse("B not found", o.containsKey("B"));
        }
        {
            Map<String, Object> o = new ObjectMapper().readValue("{\"A\":\"str\", \"B\":[1,2,3]}", Map.class);
            QueryOutput output = new QueryOutput("B");
            QueryUtils.filterFields(o, output);
            Assert.assertTrue("B found", o.containsKey("B"));
            Assert.assertFalse("A not found", o.containsKey("A"));
        }
        {
            Map<String, Object> o = new ObjectMapper()
                    .readValue("{\"A\":\"str\", \"B\":{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"}}", Map.class);
            Map<String,Object> sf = new HashMap<>();
            sf.put("B", Arrays.asList("1","2"));
            QueryOutput output = new QueryOutput(sf);
            QueryUtils.filterFields(o, output);
            Assert.assertTrue("B found", o.containsKey("B"));
            Assert.assertFalse("A not found", o.containsKey("A"));
            Assert.assertTrue("B.1 found", !DataUtils.getObjValsForKey(o, "B.1").isEmpty());
            Assert.assertTrue("B.2 found", !DataUtils.getObjValsForKey(o, "B.2").isEmpty());
            Assert.assertTrue("B.3 not found", DataUtils.getObjValsForKey(o, "B.3").isEmpty());
        }
        {
            Map<String, Object> o = new ObjectMapper()
                    .readValue("{\"A\":\"str\", \"B\":{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"}}", Map.class);
            QueryOutput output = QueryOutput.build("[\"B.1\",\"B.2\"]");
            QueryUtils.filterFields(o, output);
            Assert.assertTrue("B found", o.containsKey("B"));
            Assert.assertFalse("A not found", o.containsKey("A"));
            Assert.assertTrue("B.1 found", !DataUtils.getObjValsForKey(o, "B.1").isEmpty());
            Assert.assertTrue("B.2 found", !DataUtils.getObjValsForKey(o, "B.2").isEmpty());
            Assert.assertTrue("B.3 not found", DataUtils.getObjValsForKey(o, "B.3").isEmpty());
        }
        {
            Map<String, Object> o = new ObjectMapper()
                    .readValue("{\"A\":\"str\", \"B\":[{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"}]}", Map.class);
            QueryOutput output = QueryOutput.build("[\"B.1\",\"B.2\"]");
            QueryUtils.filterFields(o, output);
            Assert.assertTrue("B found", o.containsKey("B"));
            Assert.assertFalse("A not found", o.containsKey("A"));
            Assert.assertTrue("B.1 found", !DataUtils.getObjValsForKey(o, "B.1").isEmpty());
            Assert.assertTrue("B.2 found", !DataUtils.getObjValsForKey(o, "B.2").isEmpty());
            Assert.assertTrue("B.3 not found", DataUtils.getObjValsForKey(o, "B.3").isEmpty());
        }
        {
            Map<String, Object> o = new ObjectMapper()
                    .readValue("{\"A\":\"str\", \"B\":[{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"}]}", Map.class);
            QueryOutput output = QueryOutput.build("{\"B\":[\"1\",\"2\"]}");
            QueryUtils.filterFields(o, output);
            Assert.assertTrue("B found", o.containsKey("B"));
            Assert.assertFalse("A not found", o.containsKey("A"));
            Assert.assertTrue("B.1 found", !DataUtils.getObjValsForKey(o, "B.1").isEmpty());
            Assert.assertTrue("B.2 found", !DataUtils.getObjValsForKey(o, "B.2").isEmpty());
            Assert.assertTrue("B.3 not found", DataUtils.getObjValsForKey(o, "B.3").isEmpty());
        }
        {
            Map<String, Object> o = new ObjectMapper()
                    .readValue("{\"A\":\"str\", \"B\":{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"}}", Map.class);
            QueryOutput output = QueryOutput.build("[\"B\"]");
            QueryUtils.filterFields(o, output);
            Assert.assertTrue("B found", o.containsKey("B"));
            Assert.assertFalse("A not found", o.containsKey("A"));
            Assert.assertTrue("B.1 found", !DataUtils.getObjValsForKey(o, "B.1").isEmpty());
            Assert.assertTrue("B.2 found", !DataUtils.getObjValsForKey(o, "B.2").isEmpty());
            Assert.assertTrue("B.3 found", !DataUtils.getObjValsForKey(o, "B.3").isEmpty());
        }
        {
            Map<String, Object> o = new ObjectMapper()
                    .readValue("{\"A\":[\"str\"], \"B\":{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"}}", Map.class);
            QueryOutput output = QueryOutput.build("[\"B\"]");
            QueryUtils.filterFields(o, output);
            Assert.assertTrue("B found", o.containsKey("B"));
            Assert.assertFalse("A not found", o.containsKey("A"));
            Assert.assertTrue("B.1 found", !DataUtils.getObjValsForKey(o, "B.1").isEmpty());
            Assert.assertTrue("B.2 found", !DataUtils.getObjValsForKey(o, "B.2").isEmpty());
            Assert.assertTrue("B.3 found", !DataUtils.getObjValsForKey(o, "B.3").isEmpty());
        }
        {
            Map<String, Object> o = new ObjectMapper()
                    .readValue("{\"A\":[\"str\"], \"B\":[\"1\",\"2\",\"3\"]}", Map.class);
            QueryOutput output = QueryOutput.build("[\"B\"]");
            QueryUtils.filterFields(o, output);
            Assert.assertFalse("A not found", o.containsKey("A"));
            Assert.assertTrue("B found", o.containsKey("B"));
            Assert.assertEquals("B has 3 elems", 3, ((List)o.get("B")).size());
        }
        {
            Map<String, Object> o = new ObjectMapper()
                    .readValue("{\"A\":[\"str\"], \"B\":[{\"1\":\"a\",\"2\":\"b\",\"3\":\"c\"},{\"1\":\"x\",\"2\":\"y\",\"3\":\"z\"}]}", Map.class);
            QueryOutput output = QueryOutput.build("[\"B\"]");
            QueryUtils.filterFields(o, output);
            Assert.assertFalse("A not found", o.containsKey("A"));
            Assert.assertTrue("B found", o.containsKey("B"));
        }
    }
    
    @Test
    public void testStringMatchers() {
        List<String> vals = Arrays.asList("banana", "mango", "apple", "pineapple");
        Assert.assertTrue("banana found", QueryUtils.containsMatch(vals, "banana"));
        Assert.assertTrue("pine found", QueryUtils.containsMatch(vals, "pine"));
        Assert.assertFalse("mapple not found", QueryUtils.containsMatch(vals, "mapple"));
        Assert.assertFalse("lychee not found", QueryUtils.containsMatch(vals, "lychee"));
    }

    @Test
    public void testNumericMatchersInt() {
        BigDecimal val = new BigDecimal("99");
        Assert.assertTrue("Match", QueryUtils.numberMatch(val, "99"));
        Assert.assertTrue("GT", QueryUtils.numberMatch(val, ">98"));
        Assert.assertFalse("GT fail", QueryUtils.numberMatch(val, ">99"));
        Assert.assertTrue("LT", QueryUtils.numberMatch(val, "<100"));
        Assert.assertFalse("LT fail", QueryUtils.numberMatch(val, "<99"));
        Assert.assertTrue("GTE", QueryUtils.numberMatch(val, ">=99"));
        Assert.assertTrue("LTE", QueryUtils.numberMatch(val, "<=99"));
        Assert.assertTrue("Range", QueryUtils.numberMatch(val, "98-100"));
        Assert.assertTrue("Range", QueryUtils.numberMatch(val, "99-100"));
        Assert.assertTrue("Range", QueryUtils.numberMatch(val, "98-99"));
        Assert.assertFalse("Range", QueryUtils.numberMatch(val, "100-101"));
        Assert.assertFalse("Range", QueryUtils.numberMatch(val, "97-98"));
    }

    @Test
    public void testNumericMatchersDouble() {
        BigDecimal val = new BigDecimal("99.9");
        Assert.assertTrue("Match", QueryUtils.numberMatch(val, "99.9"));
        Assert.assertTrue("GT", QueryUtils.numberMatch(val, ">98"));
        Assert.assertFalse("GT fail", QueryUtils.numberMatch(val, ">100"));
        Assert.assertTrue("LT", QueryUtils.numberMatch(val, "<100"));
        Assert.assertFalse("LT fail", QueryUtils.numberMatch(val, "<99"));
        Assert.assertTrue("GTE", QueryUtils.numberMatch(val, ">=99.9"));
        Assert.assertTrue("LTE", QueryUtils.numberMatch(val, "<=99.9"));
        Assert.assertTrue("Range", QueryUtils.numberMatch(val, "99-100"));
        Assert.assertFalse("Range", QueryUtils.numberMatch(val, "100-101"));
        Assert.assertFalse("Range", QueryUtils.numberMatch(val, "97-98"));
    }

    @Test
    public void testTermPredicate() {
        Query q = new Query(FieldType.TERM, "fruit", "apple");
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "apple");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "pineapple");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
    }

    @Test
    public void testTermPredicates() {
        Query q = new Query(FieldType.TERM, "fruit", "apple", "pineapple");
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "apple");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "pineapple");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "banana");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
    }

    @Test
    public void testTextPredicate() {
        Query q = new Query(FieldType.TEXT, "fruit", "apple");
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "apple");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "pineapple");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "banana");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
    }

    @Test
    public void testTextPredicates() {
        Query q = new Query(FieldType.TEXT, "fruit", "apple", "banana");
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "apple");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "pineapple");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "banana");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Map<String, Object> o = new HashMap<>();
            o.put("fruit", "mango");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
    }

    @Test
    public void testNumericPredicate() {
        Map<String, Object> o = new HashMap<>();
        o.put("val", 10);
        {
            Query q = new Query(FieldType.NUMBER, "val", "10");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", ">9");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", "<11");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", "<=10");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", ">=10");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", "9-11");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", "11");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", ">10");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", "<10");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", "<=9");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", ">=11");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NUMBER, "val", "11-12");
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
    }

    @Test
    public void testNestedPredicate() {
        Map<String, Object> so = new HashMap<>();
        so.put("fruit", "apple");
        so.put("ripeness", "ripe");
        Map<String, Object> o = new HashMap<>();
        o.put("key", so);
        {
            Query q = new Query(FieldType.NESTED, "key", new Query(FieldType.TERM, "fruit", "apple"));
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NESTED, "key", new Query(FieldType.TERM, "fruit", "banana"));
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.TERM, "key.fruit", "apple");
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NESTED, "key", new Query(FieldType.TERM, "fruit", "apple"),
                    new Query(FieldType.TERM, "ripeness", "ripe"));
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NESTED, "key", new Query(FieldType.TERM, "fruit", "apple"),
                    new Query(FieldType.TERM, "ripeness", "green"));
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
    }

}
