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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.CollectionUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.springframework.web.client.RestTemplate;

/**
 * Code for retrieving sequences from the Ensembl REST service
 * 
 * @author dstaines
 *
 */
public class EnsemblRestSequenceSearch implements Search {

	public final static List<String> VALID_ARGS = Arrays.asList("type", "expand_5prime", "expand_3prime", "type",
			"format");

	public final static int DEFAULT_BATCH_SIZE = 50;

	private final String baseUrl;
	private final int batchSize;
	private final RestTemplate template = new RestTemplate();

	public EnsemblRestSequenceSearch(String baseUrl, int batchSize) {
		this.baseUrl = baseUrl;
		this.batchSize = batchSize;
	}

	public EnsemblRestSequenceSearch(String baseUrl) {
		this(baseUrl, DEFAULT_BATCH_SIZE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, java.util.List, java.lang.String, java.util.List)
	 */
	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, List<String> fieldNames,
			String target, List<Query> targetQueries) {

		// transform the query string into a URI
		String url = getPostUrl(queries);
		
		// work through IDs in batches (REST server currently only allows 50 IDs
		// at a time)
		Map<String,Object> ids = new HashMap<>();
		for (List<String> idList : CollectionUtils.eagerPartition(getIds(queries), batchSize)) {
			ids.put("ids", idList);
			// pass sequences to consumer
			template.postForObject(url, ids, List.class).stream().forEach(consumer);
		}

	}

	/**
	 * Extract any query params that need to be passed to the REST service,
	 * using VALID_ARGS as a lookup
	 * 
	 * @param queries
	 * @return
	 */
	protected String getPostUrl(List<Query> queries) {
		List<String> params = queries.stream().filter(q -> VALID_ARGS.contains(q.getFieldName()) && q.getValues().length==1)
				.map(q -> q.getFieldName() + "=" + q.getValues()[0]).collect(Collectors.toList());
		if (!params.isEmpty()) {
			return baseUrl + "?" + StringUtils.join(params, '&');
		} else {
			return baseUrl;
		}
	}

	/**
	 * extract IDs from the queries
	 * @param queries
	 * @return
	 */
	protected List<String> getIds(List<Query> queries) {
		return queries.stream().filter(q -> q.getFieldName().equals("id"))
				.flatMap(q -> Arrays.asList(q.getValues()).stream()).collect(Collectors.toList());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetchByIds(java.util.List,
	 * java.lang.String[])
	 */
	@Override
	public List<Map<String, Object>> fetchByIds(List<String> fields, String... ids) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#query(java.util.List, java.util.List,
	 * java.util.List, int, int, java.util.List, java.lang.String,
	 * java.util.List)
	 */
	@Override
	public QueryResult query(List<Query> queries, List<String> output, List<String> facets, int offset, int limit,
			List<String> sorts, String target, List<Query> targetQueries) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ensembl.genesearch.Search#fetchByIds(java.util.function.Consumer,
	 * java.lang.String[])
	 */
	@Override
	public void fetchByIds(Consumer<Map<String, Object>> consumer, String... ids) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
	 */
	@Override
	public QueryResult select(String name, int offset, int limit) {
		throw new UnsupportedOperationException();
	}

}
