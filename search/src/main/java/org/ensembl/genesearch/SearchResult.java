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

import org.ensembl.genesearch.info.FieldInfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class encapsulating a set of results (full or partial)
 * @author dstaines
 *
 */
public class SearchResult {

	protected final List<Map<String, Object>> results;
	private final transient ObjectMapper mapper = new ObjectMapper();
	protected final List<FieldInfo> fields;

	public SearchResult(List<FieldInfo> fields, List<Map<String, Object>> results) {
		this.fields = fields;
		this.results = results;
	}

	public List<Map<String, Object>> getResults() {
		return results;
	}

	public String toString() {
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public List<FieldInfo> getFields() {
		return fields;
	}

}