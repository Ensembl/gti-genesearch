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

package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.FieldType;
import org.ensembl.genesearch.output.ResultsRemodeller;

/**
 * Extension of {@link ESSearch} which flattens results to a desired level using
 * {@link ResultsRemodeller}
 * 
 * @author dstaines
 *
 */
public class ESSearchFlatten extends ESSearch {

	/**
	 * target to flatten to e.g. transcripts
	 */
	final String target;

	/**
	 * Name to use for "top-level" elements e.g. "genes"
	 */
	final String topLevel;

	/**
	 * @param client
	 * @param index
	 * @param type
	 * @param target
	 *            e.g. transcripts
	 */
	public ESSearchFlatten(Client client, String index, String type, String target, String topLevel,
			DataTypeInfo info) {
		super(client, index, type, info);
		this.target = target;
		this.topLevel = topLevel;
	}

	/**
	 * @param client
	 * @param index
	 * @param type
	 * @param target
	 *            e.g. transcripts
	 * @param scrollSize
	 * @param scrollTimeout
	 */
	public ESSearchFlatten(Client client, String index, String type, String target, String topLevel, DataTypeInfo info,
			int scrollSize, int scrollTimeout) {
		super(client, index, type, info, scrollSize, scrollTimeout);
		this.target = target;
		this.topLevel = topLevel;
	}

	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput output) {
		super.fetch(consumer, transformQueries(queries), transformOutput(output));
	}

	@Override
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {
		return super.query(transformQueries(queries), transformOutput(output), transformFields(facets), offset, limit,
				transformFields(sorts));
	}

	@Override
	protected Map<String, Map<String, Long>> processAggregations(SearchResponse response) {
		return super.processAggregations(response).entrySet().stream()
				.collect(Collectors.toMap(e -> reverseTransformField(e.getKey()), Map.Entry::getValue));
	}

	/**
	 * Transform the queries into the correct form
	 * 
	 * @param queries
	 * @return transformed queries
	 */
	protected List<Query> transformQueries(List<Query> queries) {
		List<Query> qs = new ArrayList<>();
		List<Query> targetQs = new ArrayList<>();
		for (Query q : queries) {
			if (q.getType() == FieldType.NESTED && q.getFieldName().equals(topLevel)) {
				// promote top level
				qs.addAll(Arrays.asList(q.getSubQueries()));
			} else {
				// collapse others into same form
				targetQs.add(q);
			}
		}
		qs.add(new Query(FieldType.NESTED, target, targetQs.toArray(new Query[] {})));
		return qs;
	}

	/**
	 * Transform the required output to the correct level
	 * 
	 * @param output
	 * @return transformed output
	 */
	protected QueryOutput transformOutput(QueryOutput output) {

		QueryOutput o = new QueryOutput();
		// add required prefix to toplevel e.g. id -> transcripts.id
		for(String f: output.getFields()) {
			// turn genes.genome into genes:[genome]
			if(f.startsWith(topLevel+".")) {
				o.getFields().add(f.replaceFirst(topLevel+".", StringUtils.EMPTY));
			} else {
				o.getFields().add(target + '.' + f);
			}
		}
		// append ID if not present
		String idStr = target + ".id";
		if (output.getFields().stream().anyMatch(f -> f.equals(idStr))) {
			output.getFields().add(idStr);
		}
		// promote top level element e.g. genes:[id] -> id
		for (Entry<String, QueryOutput> e : output.getSubFields().entrySet()) {
			if (e.getKey().equals(topLevel)) {
				o.getFields().addAll(e.getValue().getFields());
				o.getSubFields().putAll(e.getValue().getSubFields());
			} else {
				o.getSubFields().put(e.getKey(), e.getValue());
			}
		}
		return o;
	}

	/**
	 * Transform the required fields to the correct level (use for sorts and
	 * facets)
	 * 
	 * @param fields
	 * @return transformed fields
	 */
	protected List<String> transformFields(List<String> fields) {
		return fields.stream().map(this::transformField).collect(Collectors.toList());
	}

	protected String reverseTransformField(String field) {
		if (field.startsWith(target + '.')) {
			return field.substring(field.indexOf('.') + 1);
		} else {
			return topLevel + '.' + field;
		}
	}

	protected String transformField(String field) {
		char prefix = field.charAt(0);
		if (prefix == '+' || prefix == '-') {
			field = field.substring(1);
		}
		if (field.startsWith(topLevel + '.')) {
			field = field.substring(field.indexOf('.') + 1);
		} else {
			field = target + '.' + field;
		}
		if (prefix == '+' || prefix == '-') {
			field = prefix + field;
		}
		return field;
	}

	@Override
	protected void consumeHits(Consumer<Map<String, Object>> consumer, SearchResponse response) {
		SearchHit[] hits = response.getHits().getHits();
		StopWatch watch = new StopWatch();
		log.debug("Processing " + hits.length + " hits");
		watch.start();
		for (SearchHit hit : hits) {
			for (Map<String, Object> o : ResultsRemodeller.flatten(hitToMap(hit), target, topLevel)) {
				consumer.accept(o);
			}
		}
		watch.stop();
		log.debug("Completed processing " + hits.length + " hits in " + watch.getTime() + " ms");
	}

	@Override
	protected List<Map<String, Object>> processResults(SearchResponse response) {
		return Arrays.stream(response.getHits().getHits())
				.map(hit -> ResultsRemodeller.flatten(hitToMap(hit), target, topLevel)).flatMap(l -> l.stream())
				.collect(Collectors.toList());
	}

	@Override
	public List<FieldInfo> getFieldInfo(QueryOutput output) {
		List<FieldInfo> fields = new ArrayList<>();
		for (String field : output.getFields()) {
			if(field.startsWith(target+".")) {
				field = reverseTransformField(field);
			} else {
				field = topLevel+"."+field;
			}
			for (FieldInfo f : getDataType().getInfoForFieldName(field)) {
				if (!fields.contains(f)) {
					fields.add(f);
				}
			}
		}
		return fields;
	}
	
	
	
}
