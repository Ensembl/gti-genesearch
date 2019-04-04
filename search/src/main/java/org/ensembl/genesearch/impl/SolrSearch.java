/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ensembl.genesearch.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple {@link Search} implementation using an instance of {@link SolrClient}
 * 
 * Supports sorting but not faceting
 * 
 * @author dstaines
 *
 */
public class SolrSearch implements Search {

	private static final int PAGESIZE = 1000;
	private final SolrClient solr;
	private final DataTypeInfo dataType;
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final Optional<FieldInfo> idField;

	/**
	 * Build a new search instance using the supplied client
	 * 
	 * @param solr
	 *            client to use
	 * @param dataType
	 */
	public SolrSearch(SolrClient solr, DataTypeInfo dataType) {
		this.solr = solr;
		this.dataType = dataType;
		idField = dataType.getIdField();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, org.ensembl.genesearch.QueryOutput)
	 */
	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
		try {
			StopWatch w = new StopWatch();
			w.start();
			SolrQuery q = SolrQueryBuilder.build(queries);
			q.setFields(fieldNames.getFields().toArray(new String[] {}));
			q.set(SolrQueryBuilder.ROWS_PARAM, PAGESIZE);
			if (idField.isPresent()) {
			    // if the data type has an annotated ID, we can use a cursor (more efficient)
				paginateWithCursor(consumer, q);
			} else {
			    // otherwise, use standard pagination
				paginate(consumer, q);
			}
			log.info("Completed Solr query in " + w.getTime() + " ms");
		} catch (SolrServerException | IOException e) {
			throw new UnsupportedOperationException("Could not execute query", e);
		}
	}

	/**
	 * Utility to paginate over a complete result set and pass results to a consumer
	 * @param consumer
	 * @param q Solr query string
	 * @throws SolrServerException
	 * @throws IOException
	 */
	protected void paginate(Consumer<Map<String, Object>> consumer, SolrQuery q)
			throws SolrServerException, IOException {
		log.debug("Using standard pagination");
		long offset = 0;
		while(true) {
			q.set(SolrQueryBuilder.START_PARAM, String.valueOf(offset));
			log.info("Executing Solr query " + q);
			QueryResponse response = solr.query(q);
			response.getResults().stream().map(this::parseResult).forEach(consumer);					
			offset += PAGESIZE;
			if(offset>response.getResults().getNumFound()) {
				break;
			}
		}
	}

	/**
	 * Pagination over results using a cursor. Requires ID in the output.
	 * @param consumer
	 * @param q Solr query string
	 * @throws SolrServerException
	 * @throws IOException
	 */
	protected void paginateWithCursor(Consumer<Map<String, Object>> consumer, SolrQuery q)
			throws SolrServerException, IOException {
		log.debug("Using cursorMark");
		q.setSort(SortClause.asc(idField.get().getName()));
		log.info("Executing Solr query " + q);
		String cursorMark = CursorMarkParams.CURSOR_MARK_START;
		boolean done = false;
		while (!done) {
			q.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
			QueryResponse response = solr.query(q);
			String nextCursorMark = response.getNextCursorMark();
			response.getResults().stream().map(this::parseResult).forEach(consumer);
			if (cursorMark.equals(nextCursorMark)) {
				done = true;
			}
			cursorMark = nextCursorMark;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#query(java.util.List,
	 * org.ensembl.genesearch.QueryOutput, java.util.List, int, int,
	 * java.util.List)
	 */
	@Override
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {
		SolrQuery q = SolrQueryBuilder.build(queries);
		q.setFields(output.getFields().toArray(new String[] {}));
		q.set(SolrQueryBuilder.START_PARAM, offset);
		q.set(SolrQueryBuilder.ROWS_PARAM, limit);
		if (facets!=null && !facets.isEmpty()) {
			throw new IllegalArgumentException("SolrSearch does not currently support facets");
		}
		if (sorts!=null && !sorts.isEmpty()) {
			q.add("sort", SolrQueryBuilder.parseSorts(sorts));
		}
		try {
			StopWatch w = new StopWatch();
			w.start();
			QueryResponse response = solr.query(q);
			List<Map<String, Object>> results = response.getResults().stream().map(this::parseResult)
					.collect(Collectors.toList());
			log.info("Completed Solr query in " + w.getTime() + " ms");
			return new QueryResult(response.getResults().getNumFound(), offset, limit, getFieldInfo(output), results,
					Collections.emptyMap());
		} catch (SolrServerException | IOException e) {
			throw new UnsupportedOperationException("Could not execute query", e);
		}
	}

	/**
	 * Transform a {@link SolrDocument} into a plain old map
	 * 
	 * @param doc
	 * @return map
	 */
	private Map<String, Object> parseResult(SolrDocument doc) {
		Map<String, Object> o = new HashMap<>();
		doc.entrySet().stream().forEach(e -> o.put(e.getKey(), parseVal(e.getValue())));
		return o;
	}

	/**
	 * Function to decide if a solr document needs more parsing. May recurse
	 * 
	 * @param value
	 * @return object
	 */
	private Object parseVal(Object value) {
		if (SolrDocument.class.isAssignableFrom(value.getClass())) {
			return parseResult((SolrDocument) value);
		} else if (List.class.isAssignableFrom(value.getClass())) {
			return ((List) value).stream().map(this::parseVal).collect(Collectors.toList());
		} else {
			return value;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
	 */
	@Override
	public QueryResult select(String name, int offset, int limit) {
		throw new UnsupportedOperationException("select is not yet supported for Solr");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#getDataType()
	 */
	@Override
	public DataTypeInfo getDataType() {
		return dataType;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.Search#up()
	 */
	@Override
	public boolean up() {
		try {
			return solr.ping().getStatus()==0;
		} catch (SolrServerException | IOException e) {
			log.warn("Could not ping Solr server", e);
			return false;
		}
	}

}
