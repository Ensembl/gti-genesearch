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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.utils.QueryUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class encapsulating fields to display
 * 
 * @author dstaines
 *
 */
public class QueryOutput {

    private static final String WILD = "*";
	private static final char MAP_START = '{';
    private static final char MAP_END = '}';
    private static final char ARRAY_START = '[';
    private static final char ARRAY_END = ']';
    private static final ObjectMapper om = new ObjectMapper();

    public static QueryOutput build(Map<String, Object> fields) {
        return new QueryOutput(fields);
    }

    public static QueryOutput build(List<?> fields) {
        return new QueryOutput(fields);
    }

    public static QueryOutput build(String fieldStr) {
        if (StringUtils.isEmpty(fieldStr)) {
            return new QueryOutput();
        }
        try {
            char start = fieldStr.charAt(0);
            char end = fieldStr.charAt(fieldStr.length() - 1);
            if (start == ARRAY_START && end == ARRAY_END) {
                return new QueryOutput(om.readValue(fieldStr, List.class));
            } else if (start == MAP_START && end == MAP_END) {
                return new QueryOutput(om.readValue(fieldStr, Map.class));
            } else {
                return new QueryOutput(om.readValue(ARRAY_START + fieldStr + ARRAY_END, List.class));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not parse query output string " + fieldStr, e);
        }
    }

    private final List<String> fields = new ArrayList<>();
    private final Map<String, QueryOutput> subFields = new HashMap<>();

    public QueryOutput(List<?> f) {
        for (Object e : f) {
            if (Map.class.isAssignableFrom(e.getClass())) {
                for (Entry<String, List<Object>> e2 : ((Map<String, List<Object>>) e).entrySet()) {
                    subFields.put(e2.getKey(), QueryOutput.build(e2.getValue()));
                }
            } else {
                fields.add(String.valueOf(e).trim());
            }
        }
    }

    public QueryOutput(String... f) {
        this(Arrays.asList(f));
    }

    public QueryOutput(Map<String, Object> sF) {
        for (Entry<String, Object> f : sF.entrySet()) {
            if (List.class.isAssignableFrom(f.getValue().getClass())) {
                subFields.put(f.getKey(), new QueryOutput((List) f.getValue()));
            } else if (Map.class.isAssignableFrom(f.getValue().getClass())) {
                subFields.put(f.getKey(), new QueryOutput((Map) f.getValue()));
            } else {
                throw new IllegalArgumentException("Could not parse query output " + f.getValue());
            }
        }
    }

    public List<String> getFields() {
        return fields;
    }

    public Map<String, QueryOutput> getSubFields() {
        return subFields;
    }

    public String toString() {
        try {
            return om.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @return true if fields contains top level '*'
     */
    public boolean isWild() {
    		return getFields().stream().anyMatch(s -> s.equals(WILD));
    }

    /**
     * Utility method to find if one of the fields match the supplied path
     * @param path
     * @return true if path matched
     */
    public boolean containsPath(String path) {
        boolean contains = false;
        if (getFields().stream().anyMatch(s -> s.startsWith(path))) {
            contains = true;
        } else if (!getSubFields().isEmpty()) {
            if (getSubFields().containsKey(path)) {
                contains = true;
            } else {
                int loc = path.indexOf('.');
                if (loc != -1) {
                    QueryOutput subField = getSubFields().get(path.substring(0, loc));
                    if (subField != null && subField.containsPath(path.substring(loc + 1))) {
                        contains = true;
                    }
                }
            }
        }
        return contains;
    }

    /**
     * Utility method to find if QueryOutput contains any children of a
     * specified field. Used by {@link QueryUtils} to decide whether to filter
     * out a whole field
     * 
     * @param path
     * @return
     */
    public boolean containsPathChildren(String path) {
        boolean contains = false;
        if (containsPath(path)) {
            if (getFields().stream().anyMatch(s -> s.startsWith(path + '.'))) {
                contains = true;
            } else if (!getSubFields().isEmpty()) {
                int loc = path.indexOf('.');
                if (getSubFields().containsKey(path)) {
                    contains = true;
                } else if (loc != -1) {
                    QueryOutput subField = getSubFields().get(path.substring(0, loc));
                    if (subField != null && subField.containsPathChildren(path.substring(loc + 1))) {
                        contains = true;
                    }
                }
            }
        }
        return contains;
    }

}
