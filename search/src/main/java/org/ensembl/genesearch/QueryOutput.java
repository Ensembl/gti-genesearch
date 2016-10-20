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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class encapsulating fields to display
 * 
 * @author dstaines
 *
 */
public class QueryOutput {

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
		if(StringUtils.isEmpty(fieldStr)) {
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

}