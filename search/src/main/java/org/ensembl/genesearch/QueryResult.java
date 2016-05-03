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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class encapsulating a result set. Returned by {@link Search}.
 * @author dstaines
 *
 */
public class QueryResult {

	private final long resultCount;
	private final long offset;
	private final long limit;
	private final List<Map<String, Object>> results;
	private final Map<String, Map<String, Long>> facets;
	private final transient ObjectMapper mapper = new ObjectMapper();

	public QueryResult(long resultCount, long offset, long limit, List<Map<String, Object>> results,
			Map<String, Map<String, Long>> facets) {
		super();
		this.resultCount = resultCount;
		this.offset = offset;
		this.limit = limit;
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

	public long getOffset() {
		return offset;
	}

	public long getLimit() {
		return limit;
	}

}