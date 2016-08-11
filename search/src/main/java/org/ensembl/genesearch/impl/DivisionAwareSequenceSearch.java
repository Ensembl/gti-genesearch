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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;

/**
 * Delegating search which uses EG or e! REST depending on division for the
 * query
 * 
 * @author dstaines
 *
 */
public class DivisionAwareSequenceSearch implements Search {

	public static final String SEQUENCE = "sequence";
	public static final String ENSEMBL = "Ensembl";
	public static final String DIVISION = "division";
	public static final String ID = "id";
	private final EnsemblRestSequenceSearch eSearch;
	private final EnsemblRestSequenceSearch egSearch;

	public DivisionAwareSequenceSearch(String eSearchUri, String egSearchUri) {
		this(new EnsemblRestSequenceSearch(eSearchUri), new EnsemblRestSequenceSearch(egSearchUri));
	}

	public DivisionAwareSequenceSearch(EnsemblRestSequenceSearch eSearch, EnsemblRestSequenceSearch egSearch) {
		this.eSearch = eSearch;
		this.egSearch = egSearch;
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
		// expected a list of nested queries
		for (Query q : queries) {
			if (q.getType() != QueryType.NESTED || !ID.equals(q.getSubQueries()[0].getFieldName())) {
				throw new IllegalArgumentException("Sequence search requires a nested query containing id query");
			}
			String division = q.getFieldName();
			if(ENSEMBL.equals(division)) {
				eSearch.fetch(consumer, Arrays.asList(q.getSubQueries()), fieldNames);
			} else {
				egSearch.fetch(consumer, Arrays.asList(q.getSubQueries()), fieldNames);				
			}
		}
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
