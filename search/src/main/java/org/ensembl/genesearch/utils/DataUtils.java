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

package org.ensembl.genesearch.utils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.ensembl.genesearch.test.ESTestServer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilities for manipulating data objects
 * 
 * @author dstaines
 *
 */
public class DataUtils {

    /**
     * Find values for a given key in a data structure
     * 
     * @param r
     * @param key
     * @return set of values
     */
    public static Set<String> getObjValsForKey(Map<String, Object> r, String key) {
        return getObjsForKey(r, key).keySet();
    }

    /**
     * Find all data for a given key, keyed by the value of that key. Used to
     * invert a map by value
     * 
     * @param r
     *            data object to search
     * @param key
     *            key to search for
     * @return map containing data
     */
    public static Map<String, Map<String, Object>> getObjsForKey(Map<String, Object> r, String key) {
        Map<String, Map<String, Object>> keys = new HashMap<>();
        getObjsForKey(r, key, keys);
        return keys;
    }

    /**
     * Internal method to iteratively search an object.
     * 
     * @param r
     *            data object to search
     * @param key
     *            key to search for
     * @param keys
     *            running list of data for key
     */
    protected static void getObjsForKey(Map<String, Object> r, String key, Map<String, Map<String, Object>> keys) {
        int i = key.indexOf('.');
        if (i != -1) {
            String stem = key.substring(0, i);
            String tail = key.substring(i + 1);
            Object subObj = r.get(stem);
            if (subObj != null) {
                if (Map.class.isAssignableFrom(subObj.getClass())) {
                    getObjsForKey((Map) subObj, tail, keys);
                } else if (List.class.isAssignableFrom(subObj.getClass())) {
                    for (Object o : (List) subObj) {
                        getObjsForKey((Map) o, tail, keys);
                    }
                } else {
                    throw new IllegalArgumentException("Cannot find map for key " + stem);
                }
            }
        } else {
            Object obj = r.get(key);
            if (obj != null) {
                keys.put(String.valueOf(obj), r);
            }
        }
    }

    /**
     * Transform a JSON string into a hash map
     * 
     * @param json
     * @return string-obj map representation of JSON
     */
    public static Map<String, Object> jsonToMap(String json) {
        try {
            return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Slurp a gzipped resource into a string. Useful for reading JSON files
     * from the classpath
     * 
     * @param name
     *            resource name
     * @return contents as string
     * @throws IOException
     */
    public static String readGzipResource(String name) throws IOException {
        return IOUtils.toString(new GZIPInputStream(ESTestServer.class.getResourceAsStream(name)), Charset.defaultCharset());
    }

    /**
     * Slurp a plain resource into a string. Useful for reading JSON files from
     * the classpath
     * 
     * @param name
     *            resource name
     * @return contents as string
     * @throws IOException
     */
    public static String readResource(String name) throws IOException {
        return IOUtils.toString(ESTestServer.class.getResourceAsStream(name), Charset.defaultCharset());
    }
}
