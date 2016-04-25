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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultQueryHandler implements QueryHandler {

	private static final String SEQ_REGION = "seq_region_name";
	private static final String STRAND = "strand";
	private static final String START = "start";
	private static final String END = "end";

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
		List<Query> queries = new ArrayList<>();
		for (Entry<String, Object> query : queryObj.entrySet()) {
			String key = query.getKey();
			// possibly try another handler if we want to do something special
			// if no other handler, use this one
			Object value = query.getValue();
			Class<? extends Object> clazz = value.getClass();
			if ("location".equals(key)) {
				queries.addAll(parseLocationQuery((Map<String, Object>) value));
			} else if (Map.class.isAssignableFrom(clazz)) {
				List<Query> subQs = parseQuery((Map<String, Object>) value);
				queries.add(new Query(QueryType.NESTED, key, subQs.toArray(new Query[subQs.size()])));
			} else {
				if (List.class.isAssignableFrom(clazz)) {
					List<String> vals = ((List<Object>) value).stream().map(o -> String.valueOf(o))
							.collect(Collectors.<String> toList());
					queries.add(new Query(QueryType.TERM, key, vals));
				} else {
					queries.add(new Query(QueryType.TERM, key, String.valueOf(value)));
				}
			}
		}
		return queries;
	}

	/**
	 * Parse a location hash
	 * 
	 * @param value
	 *            hash to parse e.g. {"seq_region_name":"1", "start":"1",
	 *            "end":"1000", "strand":"1"}
	 * @return list of gene queries corresponding to that location
	 */
	protected List<Query> parseLocationQuery(Map<String, Object> value) {

		List<Query> queries = new ArrayList<>(4);
		queries.add(new Query(QueryType.TERM, SEQ_REGION, String.valueOf(value.get(SEQ_REGION))));

		if (value.containsKey(START)) {
			Long start = Long.parseLong(String.valueOf(value.get(START)));
			queries.add(new Query(QueryType.RANGE, START, start, null));
		}

		if (value.containsKey(END)) {
			Long end = Long.parseLong(String.valueOf(value.get(END)));
			queries.add(new Query(QueryType.RANGE, END, null, end));
		}

		if (value.containsKey(STRAND)) {
			queries.add(new Query(QueryType.TERM, STRAND, String.valueOf(value.get(STRAND))));
		}

		return queries;

	}

}
