package org.ensembl.genesearch.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.info.FieldType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link QueryUtils}
 * 
 * @author dstaines
 *
 */
public class QueryUtilsTest {

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
            Query q = new Query(FieldType.NESTED, "key", new Query(FieldType.TERM, "fruit", "apple"), new Query(FieldType.TERM, "ripeness", "ripe"));
            Assert.assertTrue(QueryUtils.filterResultsByQuery.test(o, q));
        }
        {
            Query q = new Query(FieldType.NESTED, "key", new Query(FieldType.TERM, "fruit", "apple"), new Query(FieldType.TERM, "ripeness", "green"));
            Assert.assertFalse(QueryUtils.filterResultsByQuery.test(o, q));
        }
    }

}
