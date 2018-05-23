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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.FieldType;

/**
 * Generic interface for searching for and retrieving objects from a backing
 * store.
 * 
 * @author dstaines
 *
 */
public interface Search {

    /**
     * default ID string
     */
    public static final String ID = "id";

    /**
     * Retrieve all results matching the supplied queries
     * 
     * @param queries
     * @param fieldNames
     *            (if empty the whole document will be returned)
     * @return set of results
     */
    public default SearchResult fetch(List<Query> queries, QueryOutput fieldNames) {
        if (queries.isEmpty()) {
            throw new UnsupportedOperationException("Fetch requires at least one query term");
        }
        final List<Map<String, Object>> results = new ArrayList<>();
        fetch(row -> results.add(row), queries, fieldNames);
        List<FieldInfo> fields = getFieldInfo(fieldNames);
        return new SearchResult(fields, results);
    }

    /**
     * Retrieve all results matching the supplied queries and process with the
     * supplied consumer
     * 
     * @param consumer
     * @param queries
     * @param fieldNames
     *            (if empty the whole document will be returned)
     */
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames);

    /**
     * Retrieve complete objects with the supplied IDs
     * 
     * @param ids
     * @return list of objects
     */
    public default List<Map<String, Object>> fetchByIds(String... ids) {
        return fetchByIds(QueryOutput.build(Collections.emptyList()), ids);
    }

    /**
     * Return objects with specified content given the supplied IDs
     * 
     * @param fields
     * @param ids
     * @return list of objects
     */
    public default List<Map<String, Object>> fetchByIds(QueryOutput fields, String... ids) {
        return fetch(Arrays.asList(new Query(FieldType.TERM, getIdField(), false, ids)), fields).getResults();
    }

    /**
     * Retrieve object with the supplied ID
     * 
     * @param id
     * @return object
     */
    public default Map<String, Object> fetchById(String id) {
        return fetchById(QueryOutput.build(Collections.emptyList()), id);
    }

    /**
     * Retrieve object with the supplied ID
     * 
     * @param fields
     *            to return
     * @param id
     *            for object
     * @return object
     */
    public default Map<String, Object> fetchById(QueryOutput fields, String id) {
        List<Map<String, Object>> genes = this.fetchByIds(fields, id);
        if (genes.isEmpty()) {
            return Collections.emptyMap();
        } else {
            return genes.get(0);
        }
    }

    /**
     * Search with the supplied queries and return a summary object containing
     * results and facets
     * 
     * @param queries
     *            list of queries to combine with AND
     * @param output
     *            source fields to include
     * @param facets
     *            fields to facet over
     * @param offset
     *            place to start in query
     * @param limit
     *            number of hits to return
     * @return result object
     */
    public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
            List<String> sorts);

    /**
     * Retrieve genes with the supplied ID and write to the consumer
     * 
     * @param consumer
     * @param ids
     */
    public default void fetchByIds(Consumer<Map<String, Object>> consumer, String... ids) {
        fetch(consumer, Arrays.asList(new Query(FieldType.TERM, getIdField(), false, ids)), new QueryOutput());
    }

    /**
     * Find objects matching the supplied string e.g. genome by name
     * 
     * @param name
     * @return set of objects
     */
    public QueryResult select(String name, int offset, int limit);

    /**
     * Get the primary data type returned by this instance
     * 
     * @return object describing data type
     */
    public DataTypeInfo getDataType();

    /**
     * Find the list of names for the given fields
     * 
     * @param fieldNames
     * @return list of fields
     */
    public default List<FieldInfo> getFieldInfo(QueryOutput fieldNames) {
        List<FieldInfo> fields = new ArrayList<>();
        for (String field : fieldNames.getPaths()) {
            for (FieldInfo f : getDataType().getInfoForFieldName(field)) {
                if (!fields.contains(f)) {
                    fields.add(f);
                }
            }
        }
        return fields;
    }

    /**
     * Field containing unique identifier for document
     * 
     * @return name of ID field
     */
    public default String getIdField() {
        return ID;
    }

    /**
     * Method for indicating if search is available
     * 
     * @return true if search is available
     */
    public boolean up();

}
