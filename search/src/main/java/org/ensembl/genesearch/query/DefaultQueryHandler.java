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
import org.ensembl.genesearch.Query.QueryType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handler to parse out queries
 * 
 * @author dstaines
 *
 */
public class DefaultQueryHandler implements QueryHandler {

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
			String key = query.getKey();
			// possibly try another handler if we want to do something special
			// if no other handler, use this one
			Object value = query.getValue();
			Class<? extends Object> clazz = value.getClass();

			if ("location".equals(key)) {
				if (List.class.isAssignableFrom(clazz)) {
					List<String> vals = ((List<Object>) value).stream().map(o -> String.valueOf(o))
							.collect(Collectors.<String> toList());
					queries.add(new Query(QueryType.LOCATION, key, vals));
				} else {
					queries.add(new Query(QueryType.LOCATION, key, String.valueOf(value)));
				}
			} else if (Map.class.isAssignableFrom(clazz)) {
				List<Query> subQs = parseQuery((Map<String, Object>) value);
				queries.add(new Query(QueryType.NESTED, key, subQs.toArray(new Query[subQs.size()])));
			} else {
				if (List.class.isAssignableFrom(clazz)) {
					List<String> vals = ((List<Object>) value).stream().map(o -> String.valueOf(o))
							.collect(Collectors.<String> toList());
					queries.add(new Query(QueryType.TERM, key, vals));
				} else if (NUMBER_PATTERN.matcher(String.valueOf(value)).matches()) {
					queries.add(new Query(QueryType.NUMBER, key, String.valueOf(value)));
				} else {
					queries.add(new Query(QueryType.TERM, key, String.valueOf(value)));
				}
			}
		}
		return queries;
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
					int m = key.indexOf('.');
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
