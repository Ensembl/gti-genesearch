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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Generic interface for searching for and retrieving objects from a backing store
 * @author dstaines
 *
 */
public interface Search {

	/**
	 * Retrieve all results matching the supplied queries
	 * 
	 * @param queries
	 * @param fieldNames
	 *            (if empty the whole document will be returned)
	 * @return
	 */
	public List<Map<String, Object>> fetch(List<Query> queries, List<String> fieldNames);
	
	/**
	 * Retrieve all results matching the supplied queries, flattening to the specified target level
	 * 
	 * @param queries
	 * @param fieldNames
	 *            (if empty the whole document will be returned)
	 * @param target level to flatten to e.g. transcripts, transcripts.translations etc.
	 * @return
	 */
	public List<Map<String, Object>> fetch(List<Query> queries, List<String> fieldNames, String target);

	/**
	 * Retrieve all results matching the supplied queries and process with the
	 * supplied consumer
	 * 
	 * @param consumer
	 * @param queries
	 * @param fieldNames
	 *            (if empty the whole document will be returned)
	 * @return
	 */
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, List<String> fieldNames);

	/**
	 * Retrieve all results matching the supplied queries and process with the
	 * supplied consumer
	 * 
	 * @param consumer
	 * @param queries
	 * @param fieldNames
	 *            (if empty the whole document will be returned)
	 * @param target level to flatten to e.g. transcripts, transcripts.translations etc.
	 * @return
	 */
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, List<String> fieldNames, String target);

	
	/**
	 * Retrieve genes with the supplied IDs
	 * 
	 * @param ids
	 * @return
	 */
	public List<Map<String, Object>> fetchByIds(String... ids);

	/**
	 * Retrieve genes with the supplied ID
	 * 
	 * @param id
	 * @return
	 */
	public Map<String, Object> fetchById(String id);

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
	 * @param target
	 *            object to flatten results onto
	 * @return
	 */
	public QueryResult query(List<Query> queries, List<String> output, List<String> facets, int offset, int limit,
			List<String> sorts, String target);

	/**
	 * Retrieve genes with the supplied ID and write to the consumer
	 * 
	 * @param consumer
	 * @param ids
	 */
	public void fetchByIds(Consumer<Map<String, Object>> consumer, String... ids);
	
	/**
	 * Find a document matching the supplied string
	 * @param name
	 * @return
	 */
	public QueryResult select(String name, int offset, int limit);

}
