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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.ensembl.genesearch.Query.GT;
import static org.ensembl.genesearch.Query.GTE;
import static org.ensembl.genesearch.Query.LT;
import static org.ensembl.genesearch.Query.LTE;
import static org.ensembl.genesearch.Query.RANGE;
import static org.ensembl.genesearch.Query.SINGLE_NUMBER;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;

/**
 * Utilities for performing server side query operations
 * 
 * @author dstaines
 *
 */
public class QueryUtils {

    /**
     * Utility to strip out all fields from a nested map that are not present in
     * the supplied output object
     * 
     * @param obj
     * @param output
     */
    public static Map<String, Object> filterFields(Map<String, Object> obj, QueryOutput output) {
        return filterFields(obj, output, null);
    }

    @SuppressWarnings("unchecked")
    protected static Map<String, Object> filterFields(Map<String, Object> obj, QueryOutput output, String path) {
    	    // if no output, or output is wild, don't filter.
    	    // wild filtering only applies to top level
        if (output == null || (StringUtils.isEmpty(path) && output.isWild())) {
            return obj;
        }
        Iterator<String> i = obj.keySet().iterator();
        while (i.hasNext()) {
            String key = i.next();
            String keyPath = key;
            if (!StringUtils.isEmpty(path)) {
                keyPath = path + '.' + key;
            }
            Object so = obj.get(key);
            if (so!=null && Map.class.isAssignableFrom(so.getClass()) && output.containsPathChildren(keyPath)) {
                Map<String, Object> mo = (Map<String, Object>) so;
                filterFields(mo, output, keyPath);
                if (mo.isEmpty()) {
                    i.remove();
                }
            } else if (so!=null && List.class.isAssignableFrom(so.getClass())) {

                List<?> lo = (List<?>) so;
                if (lo.isEmpty()) {
                    i.remove();
                } else if (Map.class.isAssignableFrom(lo.get(0).getClass())) {
                    if (output.containsPathChildren(keyPath)) {
                        Iterator<Map<String, Object>> li = ((List<Map<String, Object>>) lo).iterator();
                        while (li.hasNext()) {
                            Map<String, Object> mo = li.next();
                            filterFields(mo, output, keyPath);
                            if (mo.isEmpty()) {
                                li.remove();
                            }
                        }
                        if (lo.isEmpty()) {
                            i.remove();
                        }
                    } else if(!output.containsPath(keyPath)) {
                    		i.remove();
                    }
                } else {
                    if (!output.containsPath(keyPath)) {
                        i.remove();
                    }
                }

            } else if (!output.containsPath(keyPath)) {
                i.remove();
            }
        }
        return obj;
    }

    public static BiPredicate<Map<String, Object>, List<Query>> filterResultsByQueries = new BiPredicate<Map<String, Object>, List<Query>>() {

        @Override
        public boolean test(Map<String, Object> o, List<Query> qs) {
            boolean retain = true;
            for (Query q : qs) {
                if (!filterResultsByQuery.test(o, q)) {
                    retain = false;
                    break;
                }
            }
            return retain;
        }
    };

    public static BiPredicate<Map<String, Object>, Query> filterResultsByQuery = new BiPredicate<Map<String, Object>, Query>() {
        @Override
        public boolean test(Map<String, Object> o, Query q) {
            Set<String> vals = DataUtils.getObjValsForKey(o, q.getFieldName());
            switch (q.getType()) {
            case TERM:
                if (Arrays.stream(q.getValues()).anyMatch(qv -> vals.contains(qv))) {
                    return true;
                }
                break;
            case TEXT:
                if (Arrays.stream(q.getValues()).anyMatch(qv -> containsMatch(vals, qv))) {
                    return true;
                }
                break;
            case NUMBER:
                if (Arrays.stream(q.getValues()).anyMatch(qv -> numberMatch(vals, qv))) {
                    return true;
                }
                break;
            case NESTED:
                if (testNested(o, q)) {
                    return true;
                }
                break;
            default:
                throw new UnsupportedOperationException("Cannot filter by type " + q.getType());
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        protected boolean testNested(Map<String, Object> o, Query q) {
            Object os = o.get(q.getFieldName());
            if (os != null) {
                if (List.class.isAssignableFrom(os.getClass())) {
                    for (Map<String, Object> oo : (List<Map<String, Object>>) os) {
                        boolean passes = true;
                        for (Query sq : q.getSubQueries()) {
                            if (!test(oo, sq)) {
                                passes = false;
                            }
                        }
                        return passes;
                    }
                    return false;
                } else if (Map.class.isAssignableFrom(os.getClass())) {
                    boolean passes = true;
                    for (Query sq : q.getSubQueries()) {
                        if (!test((Map<String, Object>) os, sq)) {
                            passes = false;
                        }
                    }
                    return passes;
                } else {
                    throw new UnsupportedOperationException(
                            "Cannot process nested query using object " + os.getClass());
                }
            }
            return false;
        }

    };

    public static boolean containsMatch(Collection<String> vals, String qv) {
        return vals.stream().anyMatch(v -> v.contains(qv));
    }

    public static boolean numberMatch(Collection<String> vals, String qv) {
        return vals.stream().map(BigDecimal::new).anyMatch(v -> numberMatch(v, qv));
    }

    public static boolean numberMatch(BigDecimal value, String query) {
        Matcher m = SINGLE_NUMBER.matcher(query);
        if (m.matches()) {
            if (m.groupCount() == 1 || isEmpty(m.group(1))) {
                return new BigDecimal(query).compareTo(value) == 0;
            } else {
                int comp = value.compareTo(new BigDecimal(m.group(2)));
                switch (m.group(1)) {
                case GT:
                    return comp == 1;
                case GTE:
                    return comp == 1 || comp == 0;
                case LT:
                    return comp == -1;
                case LTE:
                    return comp == -1 || comp == 0;
                default:
                    throw new UnsupportedOperationException("Unsupported numeric operator " + m.group(1));
                }
            }
        } else {
            m = RANGE.matcher(query);
            if (m.matches()) {
                int comp_min = value.compareTo(new BigDecimal(m.group(1)));
                int comp_max = value.compareTo(new BigDecimal(m.group(2)));
                return comp_min > -1 && comp_max < 1;
            } else {
                throw new UnsupportedOperationException("Cannot parse numeric query " + value);
            }
        }
    }

}
