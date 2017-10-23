/*
 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ensembl.genesearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Tests for creation of {@link QueryOutput}
 * 
 * @author dstaines
 *
 */
public class QueryOutputTest {

    /**
     * Test method for
     * {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
     */
    @Test
    public void testBuildSimple() {
        String testStr = "\"1\",\"2\",\"3\"";
        QueryOutput o = QueryOutput.build(testStr);
        assertEquals("Checking 3 elements returned", 3, o.getFields().size());
        assertEquals("Checking element 1", "1", o.getFields().get(0));
        assertEquals("Checking element 2", "2", o.getFields().get(1));
        assertEquals("Checking element 3", "3", o.getFields().get(2));
        assertTrue("Checking subFields empty", o.getSubFields().isEmpty());
    }

    /**
     * Test method for
     * {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
     */
    @Test
    public void testBuildSimpleArray() {
        String testStr = "[\"1\",\"2\",\"3\"]";
        QueryOutput o = QueryOutput.build(testStr);
        assertEquals("Checking 3 elements returned", 3, o.getFields().size());
        assertEquals("Checking element 1", "1", o.getFields().get(0));
        assertEquals("Checking element 2", "2", o.getFields().get(1));
        assertEquals("Checking element 3", "3", o.getFields().get(2));
        assertTrue("Checking subFields empty", o.getSubFields().isEmpty());
    }

    /**
     * Test method for
     * {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
     */
    @Test
    public void testBuildSingleHash() {
        String testStr = "{\"genes\":[\"1\",\"2\",\"3\"]}";
        QueryOutput o = QueryOutput.build(testStr);
        assertEquals("Checking 0 elements returned", 0, o.getFields().size());
        assertEquals("Checking for a single subfield", 1, o.getSubFields().keySet().size());
        assertTrue("Checking for genes", o.getSubFields().keySet().contains("genes"));
        QueryOutput g = o.getSubFields().get("genes");
        assertEquals("Checking for 3 sub elems", 3, g.getFields().size());
        assertEquals("Checking element 1", "1", g.getFields().get(0));
        assertEquals("Checking element 2", "2", g.getFields().get(1));
        assertEquals("Checking element 3", "3", g.getFields().get(2));
        assertTrue("Checking subFields empty", g.getSubFields().isEmpty());
    }

    /**
     * Test method for
     * {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
     */
    @Test
    public void testBuildDoubleHash() {
        String testStr = "{\"genes\":[\"1\",\"2\",\"3\"],\"variations\":[\"A\",\"B\"]}";
        QueryOutput o = QueryOutput.build(testStr);
        assertEquals("Checking 0 elements returned", 0, o.getFields().size());
        assertEquals("Checking for 2 subfields", 2, o.getSubFields().keySet().size());
        assertTrue("Checking for genes", o.getSubFields().keySet().contains("genes"));

        QueryOutput g = o.getSubFields().get("genes");
        assertEquals("Checking for 3 sub elems", 3, g.getFields().size());
        assertEquals("Checking element 1", "1", g.getFields().get(0));
        assertEquals("Checking element 2", "2", g.getFields().get(1));
        assertEquals("Checking element 3", "3", g.getFields().get(2));
        assertTrue("Checking subFields empty", g.getSubFields().isEmpty());

        assertTrue("Checking for variations", o.getSubFields().keySet().contains("variations"));
        QueryOutput v = o.getSubFields().get("variations");
        assertEquals("Checking for 2 sub elems", 2, v.getFields().size());
        assertEquals("Checking element 1", "A", v.getFields().get(0));
        assertEquals("Checking element 2", "B", v.getFields().get(1));
        assertTrue("Checking subFields empty", v.getSubFields().isEmpty());
    }

    /**
     * Test method for
     * {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
     */
    @Test
    public void testBuildFromList() {
        QueryOutput o = QueryOutput.build(Arrays.asList("1", "2", "3"));
        assertEquals("Checking 3 elements returned", 3, o.getFields().size());
        assertEquals("Checking element 1", "1", o.getFields().get(0));
        assertEquals("Checking element 2", "2", o.getFields().get(1));
        assertEquals("Checking element 3", "3", o.getFields().get(2));
        assertTrue("Checking subFields empty", o.getSubFields().isEmpty());
    }

    /**
     * Test method for
     * {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
     */
    @Test
    public void testBuildFromMixedList() {
        List<Object> asList = new ArrayList<>();
        asList.add("1");
        asList.add("2");
        asList.add("3");
        Map<String, Object> map = new HashMap<>();
        map.put("A", Arrays.asList("4", "5"));
        asList.add(map);
        QueryOutput o = QueryOutput.build(asList);
        assertEquals("Checking 3 elements returned", 3, o.getFields().size());
        assertEquals("Checking element 1", "1", o.getFields().get(0));
        assertEquals("Checking element 2", "2", o.getFields().get(1));
        assertEquals("Checking element 3", "3", o.getFields().get(2));
        assertEquals("Checking subFields contains 1 element", 1, o.getSubFields().size());
        QueryOutput elemA = o.getSubFields().get("A");
        assertEquals("Checking 3 elements returned", 2, elemA.getFields().size());
        assertEquals("Checking element 1", "4", elemA.getFields().get(0));
        assertEquals("Checking element 2", "5", elemA.getFields().get(1));
    }

    /**
     * Test method for
     * {@link org.ensembl.genesearch.QueryOutput#build(java.lang.String)}.
     */
    @Test
    public void testBuildFromMap() {

        Map<String, Object> map = new HashMap<>();
        map.put("genes", Arrays.asList("1", "2", "3"));
        map.put("variations", Arrays.asList("A", "B"));
        QueryOutput o = QueryOutput.build(map);
        assertEquals("Checking 0 elements returned", 0, o.getFields().size());
        assertEquals("Checking for 2 subfields", 2, o.getSubFields().keySet().size());
        assertTrue("Checking for genes", o.getSubFields().keySet().contains("genes"));

        QueryOutput g = o.getSubFields().get("genes");
        assertEquals("Checking for 3 sub elems", 3, g.getFields().size());
        assertEquals("Checking element 1", "1", g.getFields().get(0));
        assertEquals("Checking element 2", "2", g.getFields().get(1));
        assertEquals("Checking element 3", "3", g.getFields().get(2));
        assertTrue("Checking subFields empty", g.getSubFields().isEmpty());

        assertTrue("Checking for variations", o.getSubFields().keySet().contains("variations"));
        QueryOutput v = o.getSubFields().get("variations");
        assertEquals("Checking for 2 sub elems", 2, v.getFields().size());
        assertEquals("Checking element 1", "A", v.getFields().get(0));
        assertEquals("Checking element 2", "B", v.getFields().get(1));
        assertTrue("Checking subFields empty", v.getSubFields().isEmpty());
    }

    @Test
    public void testBuildFromMixedJson() {
        String fieldStr = "[\"_id\",\"name\",{\"transcripts\":[]}]";
        QueryOutput o = QueryOutput.build(fieldStr);
    }

    @Test
    public void testContainsPath() {
        {
            String testStr = "[\"1\",\"2\",\"3\"]";
            QueryOutput o = QueryOutput.build(testStr);
            assertTrue("1 found", o.containsPath("1"));
            assertTrue("2 found", o.containsPath("2"));
            assertTrue("3 found", o.containsPath("3"));
            assertFalse("4 not found", o.containsPath("4"));
            assertFalse("sub 1 not found", o.containsPath("1.1"));
        }
        {
            String testStr = "{\"genes\":[\"1\",\"2\",\"3\"],\"variations\":[\"A\",\"B\"]}";
            QueryOutput o = QueryOutput.build(testStr);
            assertTrue("genes found", o.containsPath("genes"));
            assertTrue("1 found", o.containsPath("genes.1"));
            assertTrue("2 found", o.containsPath("genes.2"));
            assertTrue("3 found", o.containsPath("genes.3"));
            assertFalse("4 not found", o.containsPath("genes.4"));
            assertTrue("variations found", o.containsPath("variations"));
            assertTrue("A found", o.containsPath("variations.A"));
            assertTrue("B found", o.containsPath("variations.B"));
            assertFalse("C not found", o.containsPath("variations.C"));
        }
        {
            QueryOutput o = new QueryOutput("genes.1","genes.2","genes.3");
            assertTrue("genes found", o.containsPath("genes"));
            assertTrue("1 found", o.containsPath("genes.1"));
            assertTrue("2 found", o.containsPath("genes.2"));
            assertTrue("3 found", o.containsPath("genes.3"));
            assertFalse("4 not found", o.containsPath("genes.4"));
        }
        {
            QueryOutput o = new QueryOutput("genes");
            assertTrue("genes found", o.containsPath("genes"));
            assertTrue("1 found", o.containsPath("genes.1"));
            assertTrue("2 found", o.containsPath("genes.2"));
            assertTrue("3 found", o.containsPath("genes.3"));
            assertTrue("4 not found", o.containsPath("genes.4"));
        }
    }

}
