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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delegating search which uses EG or e! REST depending on division for the
 * query
 * 
 * @author dstaines
 *
 */
public class DivisionAwareSequenceSearch implements Search {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	public static final String SEQUENCE = "sequence";
	public static final String SPECIES = "species";
	public static final String ENSEMBL = "Ensembl";
	public static final String GENOME = "genome";
	public static final String DIVISION = "division";
	private final Search genomeSearch;
	private final EnsemblRestSequenceSearch eSearch;
	private final EnsemblRestSequenceSearch egSearch;
	protected Set<String> isEnsembl;

	public DivisionAwareSequenceSearch(Search genomeSearch, DataTypeInfo dataType, String eSearchUri,
			String egSearchUri) {
		this(genomeSearch, new EnsemblRestSequenceSearch(eSearchUri, dataType),
				new EnsemblRestSequenceSearch(egSearchUri, dataType));
	}

	public DivisionAwareSequenceSearch(Search genomeSearch, EnsemblRestSequenceSearch eSearch,
			EnsemblRestSequenceSearch egSearch) {
		this.genomeSearch = genomeSearch;
		this.eSearch = eSearch;
		this.egSearch = egSearch;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, org.ensembl.genesearch.QueryOutput)
	 */
	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {

		// expected a list of nested queries
		for (Query q : queries) {
			if (q.getType() != QueryType.NESTED || !ID.equals(q.getSubQueries()[0].getFieldName())) {
				throw new IllegalArgumentException("Sequence search requires a nested query containing id query");
			}
			String genome = q.getFieldName();
			if (isEnsembl(genome)) {
				log.info("Dispatching " + genome + " to E");
				eSearch.fetch(consumer, Arrays.asList(q.getSubQueries()), fieldNames);
			} else {
				log.info("Dispatching " + genome + " to EG");
				egSearch.fetch(consumer, Arrays.asList(q.getSubQueries()), fieldNames);
			}
		}
	}

	protected boolean isEnsembl(String genome) {
		if (isEnsembl == null) {
			isEnsembl = new HashSet<>();
			genomeSearch.fetch(g -> {
				if (ENSEMBL.equals(g.get(DIVISION))) {
					isEnsembl.add((String) g.get(ID));
				}
			}, Collections.emptyList(), QueryOutput.build(Arrays.asList(ID, DIVISION)));
		}
		return isEnsembl.contains(genome);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetchByIds(java.util.List,
	 * java.lang.String[])
	 */
	@Override
	public List<Map<String, Object>> fetchByIds(QueryOutput fields, String... ids) {
		throw new UnsupportedOperationException();
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
	 * @see org.ensembl.genesearch.Search#getDataType()
	 */
	@Override
	public DataTypeInfo getDataType() {
		return eSearch.getDataType();
	}

}
