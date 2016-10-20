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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.CollectionUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.JsonDataTypeInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Code for retrieving sequences from the Ensembl REST service
 * 
 * @author dstaines
 *
 */
public class EnsemblRestSequenceSearch implements Search {

	public final static List<String> VALID_ARGS = Arrays.asList("type", "expand_5prime", "expand_3prime", "type",
			"format", "species");

	public final static int DEFAULT_BATCH_SIZE = 50;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private final String baseUrl;
	private final int batchSize;
	private final RestTemplate template = new RestTemplate();
	private final List<DataTypeInfo> dataTypes;

	public EnsemblRestSequenceSearch(String baseUrl, int batchSize) {
		this.baseUrl = baseUrl;
		this.batchSize = batchSize;
		try {
			this.dataTypes = JsonDataTypeInfoProvider.load("/sequence_datatype_info.json").getAll();
		} catch (IOException e) {
			// cannot recover from this
			throw new RuntimeException("Cannot load JSON from sequence_datatype_info.json");
		}
	}

	public EnsemblRestSequenceSearch(String baseUrl) {
		this(baseUrl, DEFAULT_BATCH_SIZE);
	}

	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer, java.util.List, org.ensembl.genesearch.QueryOutput)
	 */
	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {

		// transform the query string into a URI
		String url = getPostUrl(queries);
		log.info("Using base URL " + url);
		List<String> ids = getIds(queries);
		log.info("Searching for " + ids.size() + " ids");
		// work through IDs in batches (REST server currently only allows 50 IDs
		// at a time)
		Map<String, Object> idParams = new HashMap<>();
		int n = 0;
		for (List<String> idList : CollectionUtils.eagerPartition(ids, batchSize)) {
			idParams.put("ids", idList);
			n += idList.size();
			// pass sequences to consumer
			log.debug("Posting " + idList.size() + " IDs to " + url);
			template.postForObject(url, idParams, List.class).stream().forEach(consumer);
		}
		log.info("Completed querying " + n + " IDs");

	}

	@Override
	public List<FieldInfo> getFieldInfo(QueryOutput fields) {
		return getDataTypes().get(0).getFieldInfo();
	}

	/**
	 * Extract any query params that need to be passed to the REST service,
	 * using VALID_ARGS as a lookup
	 * 
	 * @param queries
	 * @return
	 */
	protected String getPostUrl(List<Query> queries) {
		List<String> params = queries.stream()
				.filter(q -> VALID_ARGS.contains(q.getFieldName()) && q.getValues().length == 1)
				.map(q -> q.getFieldName() + "=" + q.getValues()[0]).collect(Collectors.toList());
		if (!params.isEmpty()) {
			return baseUrl + "?" + StringUtils.join(params, '&');
		} else {
			return baseUrl;
		}
	}

	/**
	 * extract IDs from the queries
	 * 
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

	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.Search#query(java.util.List, org.ensembl.genesearch.QueryOutput, java.util.List, int, int, java.util.List)
	 */
	@Override
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#getDataTypes()
	 */
	@Override
	public List<DataTypeInfo> getDataTypes() {
		return dataTypes;
	}

}
