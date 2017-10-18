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

package org.ensembl.genesearch.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.info.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handler to parse out queries
 * 
 * @author dstaines
 *
 */
public class DefaultQueryHandler implements QueryHandler {
	
	protected Logger log = LoggerFactory.getLogger(this.getClass());

	private static final Pattern NUMBER_PATTERN = Pattern.compile("([<>]=?)?-?[0-9.]+(--?[0-9.]+)?");

	@Override
	public List<Query> parseQuery(String json) {
		if (StringUtils.isEmpty(json)) {
			return Collections.emptyList();
		}
		try {
			Map<String, Object> query = new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {
			});
			return parseQuery(query);
		} catch (IOException e) {
			throw new QueryHandlerException("Could not parse query string " + json, e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Query> parseQuery(Map<String, Object> queryObj) {
		queryObj = mergeQueries(queryObj);
		List<Query> queries = new ArrayList<>();
		for (Entry<String, Object> query : queryObj.entrySet()) {
			String fieldName = query.getKey();
			boolean not;
			if(fieldName.charAt(0) == '!') {
				not = true;
				fieldName = fieldName.substring(1);
			} else {
				not = false;
			}
			FieldType type = getFieldType(fieldName, query.getValue());
			if(type == FieldType.NESTED) {
				List<Query> subQs = parseQuery((Map<String, Object>) query.getValue());
				queries.add(new Query(type, fieldName, not, subQs.toArray(new Query[subQs.size()])));
			} else if(isList(query.getValue())) {
				List<String> vals = ((List<Object>) query.getValue()).stream().map(String::valueOf)
						.collect(Collectors.<String> toList());
				queries.add(new Query(type, fieldName, not, vals));
			} else {
				queries.add(new Query(type, fieldName, not, String.valueOf(query.getValue())));
			}
		}
		return queries;
	}
	
	protected FieldType getFieldType(String key, Object value) {
		if ("location".equals(key)) {
			return FieldType.LOCATION;
		} else if (Map.class.isAssignableFrom(value.getClass())) {
			return FieldType.NESTED;
		} else {
			if (isList(value)) {
				return FieldType.TERM;
			} else if (NUMBER_PATTERN.matcher(String.valueOf(value)).matches()) {
				return FieldType.NUMBER;
			} else {
				return FieldType.TERM;
			}
		}
	}
	
	protected boolean isList(Object value) {
		return List.class.isAssignableFrom(value.getClass());
	}

	/**
	 * Merge queries of the form x.y,x.z into one of the form x:{y,z}
	 * 
	 * @param input
	 *            to merge
	 * @return merged query
	 */
	public static Map<String, Object> mergeQueries(Map<String, Object> input) {
		Map<String, Object> output = new HashMap<>();
		List<String> keys = new ArrayList<>();
		keys.addAll(input.keySet());
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			Object val = input.get(key);
			int n = key.indexOf('.');
			if (n != -1) {
				String keyStem = key.substring(0, n);
				for (int j = i + 1; j < keys.size(); j++) {
					String key2 = keys.get(j);
					int m = key2.indexOf('.');
					if (m != -1) {
						String keyStem2 = key2.substring(0, m);
						if (keyStem.equals(keyStem2)) {
							// change value to be a map
							Map<String, Object> newVal = new HashMap<>();
							// put values in with remainder of stem
							newVal.put(key.substring(n + 1), val);
							newVal.put(key2.substring(m + 1), input.get(key2));
							// recurse to deal with nesting
							val = mergeQueries(newVal);
							// change key
							key = keyStem;
							// remove second value
							keys.remove(j);
						}
					}
				}
			}
			output.put(key, val);
		}
		return output;
	}

}
