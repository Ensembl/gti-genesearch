package org.ensembl.genesearch.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneQuery.GeneQueryType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultQueryHandler implements QueryHandler {

	@Override
	public List<GeneQuery> parseQuery(String json) {
		try {
			Map<String, Object> query = new ObjectMapper().readValue(json,
					new TypeReference<Map<String, Object>>() {
					});
			return parseQuery(query);
		} catch (IOException e) {
			throw new QueryHandlerException("Could not parse query string "
					+ json, e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<GeneQuery> parseQuery(Map<String, Object> queryObj) {
		List<GeneQuery> queries = new ArrayList<>();
		for (Entry<String, Object> query : queryObj.entrySet()) {
			String key = query.getKey();
			// possibly try another handler if we want to do something special
			// if no other handler, use this one
			Object value = query.getValue();
			Class<? extends Object> clazz = value.getClass();
			if (Map.class.isAssignableFrom(clazz)) {
				List<GeneQuery> subQs = parseQuery((Map<String, Object>) value);
				queries.add(new GeneQuery(GeneQueryType.NESTED, key, subQs
						.toArray(new GeneQuery[subQs.size()])));
			} else {
				if (List.class.isAssignableFrom(clazz)) {
					List<String> vals = ((List<Object>) value).stream()
							.map(o -> String.valueOf(o))
							.collect(Collectors.<String> toList());
					queries.add(new GeneQuery(GeneQueryType.TERM, key, vals));
				} else {
					queries.add(new GeneQuery(GeneQueryType.TERM, key, String
							.valueOf(value)));
				}
			}
		}
		return queries;
	}

}
