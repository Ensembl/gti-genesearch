package org.ensembl.genesearch;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class QueryResult {

	private final long resultCount;
	private final List<Map<String, Object>> results;
	private final Map<String, Map<String, Long>> facets;
	private final transient ObjectMapper mapper = new ObjectMapper();

	public QueryResult(long resultCount, List<Map<String, Object>> results,
			Map<String, Map<String, Long>> facets) {
		super();
		this.resultCount = resultCount;
		this.results = results;
		this.facets = facets;
	}

	public long getResultCount() {
		return resultCount;
	}

	public List<Map<String, Object>> getResults() {
		return results;
	}

	public Map<String, Map<String, Long>> getFacets() {
		return facets;
	}

	public String toString() {
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}