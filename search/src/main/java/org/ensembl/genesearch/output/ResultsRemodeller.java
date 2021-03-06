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

package org.ensembl.genesearch.output;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Class for flattening results to a given path/level. This is used for
 * transforming a list of genes (with transcript children) into a list of
 * children.
 * 
 * @author dstaines
 *
 */
public class ResultsRemodeller {

    /**
     * Flatten the supplied object to the desired level, capturing top level
     * elems
     * 
     * @param input
     * @param targetPath
     *            e.g. a.b will flatten to b children of a children of the
     *            supplied object
     * @param topLevel
     *            e.g. name for top level elems
     * @return object as list
     */
    public static List<Map<String, Object>> flatten(Map<String, Object> input, String targetPath, String topLevel) {
        List<Map<String, Object>> flat = flatten(Arrays.asList(input), StringUtils.EMPTY,
                StringUtils.split(targetPath, "."));
        Map<String, Object> newMap = new HashMap<>();
        flat.stream().forEach(r -> {
            Iterator<Entry<String, Object>> i = r.entrySet().iterator();
            while (i.hasNext()) {
                Entry<String, Object> e = i.next();
                String newKey = null;
                if (e.getKey().startsWith(targetPath)) {
                    newKey = e.getKey().substring(e.getKey().indexOf('.') + 1);
                } else {
                    newKey = topLevel + '.' + e.getKey();
                }
                newMap.put(newKey, e.getValue());
                i.remove();
            }
            r.putAll(newMap);
            newMap.clear();
        });
        return flat;
    }

    /**
     * Flatten the supplied object to the desired level
     * 
     * @param input
     * @param targetPath
     *            e.g. a.b will flatten to b children of a children of the
     *            supplied object
     * @return object as list
     */
    public static List<Map<String, Object>> flatten(Map<String, Object> input, String targetPath) {
        return flatten(Arrays.asList(input), StringUtils.EMPTY, StringUtils.split(targetPath, "."));
    }

    /**
     * Flatten the supplied objects to the desired level. Invoked recursively.
     * 
     * @param input
     * @param baseKey
     *            current path
     * @param targetPath
     * @return list of flattened object
     */
    protected static List<Map<String, Object>> flatten(List<Map<String, Object>> input, String baseKey,
            String... targetPath) {
        if (targetPath.length == 0) {
            return input;
        }
        List<Map<String, Object>> output = new ArrayList<>();
        // work out the current path e.g. b, b.c, b.c.d
        if (StringUtils.isEmpty(baseKey)) {
            baseKey = targetPath[0];
        } else {
            baseKey = baseKey + "." + targetPath[0];
        }
        for (Map<String, Object> obj : input) {
            Object subobj = obj.get(baseKey);
            if (subobj != null && subobj instanceof List) {
                for (Map<String, Object> o : (List<Map<String, Object>>) subobj) {
                    // create a new object for this row from the parent
                    Map<String, Object> newObj = cloneObject(obj, Arrays.asList(baseKey));
                    for (Entry<String, Object> e : o.entrySet()) {
                        newObj.put(baseKey + "." + e.getKey(), e.getValue());
                    }
                    output.add(newObj);
                }
            } else {
                output.add(obj);
            }
        }
        if (targetPath.length > 1) {
            // if we still have flattening levels, recursively invoke for the
            // remaining levels
            return flatten(output, baseKey, ArrayUtils.subarray(targetPath, 1, targetPath.length));
        } else {
            // otherwise return the flattened arrays
            return output;
        }
    }

    /**
     * Recursive deep copy mechanism with exclusion list
     * 
     * @param target
     * @param exclude
     * @return copy of supplied object
     */
    protected static Map<String, Object> cloneObject(Map<String, Object> target, Collection<String> exclude) {
        Map<String, Object> output = new HashMap<>(target.size());
        for (Entry<String, Object> e : target.entrySet()) {
            if (!exclude.contains(e.getKey()) && e.getValue() != null) {
                if (e.getValue() instanceof List) {
                    List<Object> newObjs = new ArrayList<>();
                    for (Object o : (List<? extends Object>) e.getValue()) {
                        if (o instanceof Map) {
                            newObjs.add(cloneObject((Map<String, Object>) o, exclude));
                        } else {
                            newObjs.add(o);
                        }
                    }
                    output.put(e.getKey(), newObjs);
                } else if (e.getValue() instanceof Map) {
                    output.put(e.getKey(), cloneObject((Map<String, Object>) e.getValue(), exclude));
                } else {
                    output.put(e.getKey(), e.getValue());
                }
            }
        }
        return output;
    }

}
