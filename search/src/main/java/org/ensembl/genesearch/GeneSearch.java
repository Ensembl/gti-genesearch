package org.ensembl.genesearch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface GeneSearch {

	/**
	 * Retrieve all results matching the supplied queries
	 * 
	 * @param queries
	 * @param fieldNames
	 *            (if empty the whole document will be returned)
	 * @return
	 */
	public List<Map<String, Object>> fetch(List<GeneQuery> queries,
			List<String> fieldNames);

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
	public void fetch(Consumer<Map<String, Object>> consumer,
			List<GeneQuery> queries, List<String> fieldNames);
	
	/**
	 * Retrieve genes with the supplied IDs
	 * @param ids
	 * @return
	 */
	public List<Map<String,Object>> fetchByIds(String... ids);

	/**
	 * Retrieve genes with the supplied ID
	 * @param id
	 * @return
	 */
	public Map<String,Object> fetchById(String id);

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
	 * @param limit
	 *            number of hits to return
	 * @return
	 */
	public QueryResult query(List<GeneQuery> queries,
			List<String> output, List<String> facets, int limit,
			List<String> sorts);

	/**
	 * Retrieve genes with the supplied ID and write to the consumer
	 * @param consumer
	 * @param ids
	 */
	public void fetchByIds(Consumer<Map<String, Object>> consumer, String... ids);

}
