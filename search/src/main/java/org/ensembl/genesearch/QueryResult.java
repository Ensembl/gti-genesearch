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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.ensembl.genesearch.info.FieldInfo;

/**
 * Class encapsulating a limited query result set. Returned by
 * {@link Search#query(List, QueryOutput, List, int, int, List)}
 * 
 * @author dstaines
 *
 */
public class QueryResult extends SearchResult {

    private final long resultCount;
    private final long offset;
    private final long limit;
    private final Map<String, Map<String, Long>> facets;

    public QueryResult(long resultCount, long offset, long limit, List<FieldInfo> fields,
            List<Map<String, Object>> results, Map<String, Map<String, Long>> facets) {
        super(fields, results);
        this.resultCount = resultCount;
        this.offset = offset;
        this.limit = limit;
        this.facets = facets;
    }

    /**
     * Returns facets if requested/supported. Key is field name and value is map
     * of values to counts.
     * 
     * @return map of facets
     */
    public Map<String, Map<String, Long>> getFacets() {
        return facets;
    }

    /**
     * @return offset (page start) specified when querying
     */
    public long getOffset() {
        return offset;
    }

    /**
     * @return limit (page size) specified when querying
     */
    public long getLimit() {
        return limit;
    }

    /**
     * @return number of results in whole set
     */
    public long getResultCount() {
        return resultCount;
    }

    /**
     * Render the results as a 2D list. Used for a more compact/convenient export mechanism
     * 
     * @return results as list of lists
     */
    public List<Object> getResultsAsList() {
        return results.stream().map(r -> getFields().stream().map(f -> r.get(f.getName())).collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    /**
     * Render results as map
     * 
     * @param resultsAsArray
     *            if true, results are rendered as an array
     * @return
     */
    public Map<String, Object> toMap(boolean resultsAsArray) {
        Map<String, Object> map = new LinkedHashMap<>(6);
        map.put("resultCount", resultCount);
        map.put("offset", offset);
        map.put("limit", limit);
        map.put("fields", getFields());
        if (resultsAsArray) {
            map.put("results", getResultsAsList());
        } else {
            map.put("results", results);
        }
        map.put("facets", facets);
        return map;
    }

}