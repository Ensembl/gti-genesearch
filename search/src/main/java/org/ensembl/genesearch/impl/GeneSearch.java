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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author dstaines
 *
 */
public class GeneSearch implements Search {

	protected static class SubSearchParams {
		final Optional<SearchType> name;
		final List<Query> queries;
		final QueryOutput fields;

		private SubSearchParams(SearchType name, List<Query> queries, QueryOutput fields) {
			this.name = Optional.ofNullable(name);
			this.queries = queries;
			this.fields = fields;
		}

		public final static SubSearchParams build(SearchType name, List<Query> queries, QueryOutput fields) {
			return new SubSearchParams(name, queries, fields);
		}

		public final static SubSearchParams build(String name, List<Query> queries, QueryOutput fields) {
			return new SubSearchParams(SearchType.findByName(name), queries, fields);
		}
	}

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	protected final List<DataTypeInfo> dataTypes = new ArrayList<>();
	protected final SearchRegistry provider;

	/**
	 * Search types for which we need a proper join
	 */
	protected final Set<String> joinTargets = new HashSet<>();

	/**
	 * Search types for which we can use the same search and flatten the data
	 */
	protected final Set<String> passThroughTargets = new HashSet<>();

	/**
	 * primary search
	 */
	protected final Search primarySearch;

	protected final Map<String, String> fromJoinField = new HashMap<>();
	protected final Map<String, String> toJoinField = new HashMap<>();

	public GeneSearch(SearchRegistry provider) {
		// super(provider);
		primarySearch = provider.getSearch(SearchType.GENES);
		dataTypes.addAll(primarySearch.getDataTypes());
		passThroughTargets.add(SearchType.TRANSCRIPTS.name().toLowerCase());
		passThroughTargets.add(SearchType.TRANSLATIONS.name().toLowerCase());
		Search homologSearch = provider.getSearch(SearchType.HOMOLOGUES);
		if (homologSearch != null) {
			dataTypes.addAll(homologSearch.getDataTypes());
			String homologues = SearchType.HOMOLOGUES.name().toLowerCase();
			joinTargets.add(homologues);
			fromJoinField.put(homologues, "homologues.stable_id");
			toJoinField.put(homologues, "id");
		}
		Search seqSearch = provider.getSearch(SearchType.SEQUENCES);
		if (seqSearch != null) {
			dataTypes.addAll(seqSearch.getDataTypes());
			String seqs = SearchType.SEQUENCES.name().toLowerCase();
			joinTargets.add(seqs);
			fromJoinField.put(seqs, "id");
			toJoinField.put(seqs, "id");
		}
		Search genomeSearch = provider.getSearch(SearchType.GENOMES);
		if (genomeSearch != null) {
			dataTypes.addAll(genomeSearch.getDataTypes());
			String genomes = SearchType.GENOMES.name().toLowerCase();
			joinTargets.add(genomes);
			fromJoinField.put(genomes, "genome");
			toJoinField.put(genomes, "id");
		}
		this.provider = provider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, org.ensembl.genesearch.QueryOutput)
	 */
	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
		// TODO Auto-generated method stub
		// same as above, but batch it...

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetchByIds(java.util.List,
	 * java.lang.String[])
	 */
	@Override
	public List<Map<String, Object>> fetchByIds(List<String> fields, String... ids) {
		// TODO Auto-generated method stub
		return null;
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

		// split up queries and fields
		Pair<SubSearchParams, SubSearchParams> qf = decomposeQueryFields(queries, output);

		SubSearchParams from = qf.getLeft();
		SubSearchParams to = qf.getRight();

		if (!to.name.isPresent() || passThroughTargets.contains(to.name.get())) {

			// we either have no target, or the target is a passthrough
			log.debug("Passing query through to primary search");
			return primarySearch.query(queries, output, facets, offset, limit, sorts);

			// TODO how do we do flattening here? Need a target after all
			// perhaps...
			// actually, what we'd do is to attack this flattening via another
			// end point e.g. TranscriptSearch

		} else if (joinTargets.contains(to.name.get())) {

			log.debug("Executing join query through to primary search with flattening");

			// find the to/from join fields and add to the output
			String fromField = fromJoinField.get(to.name.get());
			String toField = toJoinField.get(to.name.get());
			from.fields.getFields().add(fromField);
			to.fields.getFields().add(toField);

			// query from first and generate a set of results
			QueryResult fromResults = primarySearch.query(from.queries, from.fields, facets, offset, limit,
					sorts);

			// hash results by ID and also create a new "to" search
			Map<String, List<Map<String, Object>>> resultsById = new HashMap<>();
			Set<String> ids = new HashSet<>();
			fromResults.getResults().stream().forEach(r -> {
				String fromId = (String) r.get(fromField);
				List<Map<String, Object>> resultsForId = resultsById.get(fromId);
				if (resultsForId == null) {
					resultsForId = new ArrayList<>();
					resultsById.put(fromId, resultsForId);
				}
				resultsForId.add(r);
				ids.add((String) r.get(fromField));
			});

			// additional query joining to "to"
			to.queries.add(new Query(QueryType.TERM, toField, ids));

			// run query on "to" and map values over
			provider.getSearch(to.name.get()).fetch(r -> {
				String id = (String) r.get(toField);
				resultsById.get(id).stream().forEach(fromR -> {
					fromR.put(to.name.get().name().toLowerCase(), fromR);
				});

			}, to.queries, to.fields);

		} else {
			throw new IllegalArgumentException("Do not know how to deal with search target " + to.name.get());
		}

		return null;
	}

	/**
	 * @param output
	 * @return
	 */
	protected Pair<SubSearchParams, SubSearchParams> decomposeQueryFields(List<Query> queries, QueryOutput output) {
		String fromName = null;
		List<Query> fromQueries = new ArrayList<>();
		QueryOutput fromOutput = null;
		String toName = null;
		List<Query> toQueries = new ArrayList<>();
		QueryOutput toOutput = null;
		// decomposition depends on the QueryOutput being one of the matched 
		boolean isSimple = true;
		// do we have proper join targets?
		for(String name: joinTargets) {
			if(output.getSubFields().containsKey(name)) {
				isSimple = false;
				toName = name;
				break;
			}
		}
		if(isSimple) {
			// if not do we have passthrough join targets?
			for(String name: passThroughTargets) {
				if(output.getSubFields().containsKey(name)) {
					isSimple = false;
					toName = name;
					break;
				}				
			}
		}

		if(isSimple) {
			fromName = SearchType.GENES.name().toLowerCase();
		    fromQueries = queries;
			fromOutput = output;
		} else {
			
		}
		
		return Pair.of(SubSearchParams.build(fromName, fromQueries, fromOutput),
				SubSearchParams.build(toName, toQueries, toOutput));
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
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
	 */
	@Override
	public QueryResult select(String name, int offset, int limit) {
		// TODO Auto-generated method stub
		return null;
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
