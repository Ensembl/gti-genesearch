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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Query.QueryType;
import org.ensembl.genesearch.info.DataTypeInfo;

/**
 * @author dstaines
 *
 */
public class GeneSearch extends JoinAwareSearch {

	@Override
	protected List<Query> generateJoinQuery(SearchType joinType, List<Query> queries, List<Query> targetQueries) {

		if (joinType.equals(SearchType.SEQUENCES)) {
			Map<String, List<String>> genomeQs = new HashMap<>();
			List<String> fields = getFromJoinFields(joinType);
			int maxSize = maxJoinSize(joinType);
			// collate IDs by division
			provider.getSearch(getDefaultType()).fetch(doc -> {
				String genome = (String) doc.get("genome");
				List<String> vals = genomeQs.get(genome);
				if (vals == null) {
					vals = new ArrayList<>();
					genomeQs.put(genome, vals);
				}
				Object val = doc.get("id");
				if (val != null) {
					if (Collection.class.isAssignableFrom(val.getClass())) {
						vals.addAll((Collection) val);
					} else {
						vals.add(val.toString());
					}
					if (vals.size() > maxSize) {
						throw new RuntimeException("Can only join a maximum of " + maxSize + " " + joinType.name());
					}
				}
			}, queries, fields, null, Collections.emptyList());
			List<Query> qs = new ArrayList<>();
			for (Entry<String, List<String>> e : genomeQs.entrySet()) {
				// for each division, create a separate nested query
				List<Query> sqs = new ArrayList<>(2 + targetQueries.size());
				sqs.add(new Query(QueryType.TERM, DivisionAwareSequenceSearch.ID, e.getValue()));
				sqs.add(new Query(QueryType.TERM, DivisionAwareSequenceSearch.SPECIES, e.getKey()));
				sqs.addAll(targetQueries);
				qs.add(new Query(QueryType.NESTED, e.getKey(),
						sqs.toArray(new Query[sqs.size()])));
			}
			return qs;
		} else {
			return super.generateJoinQuery(joinType, queries, targetQueries);
		}
	}

	private static final Set<SearchType> PASSTHROUGH = new HashSet<>(
			Arrays.asList(SearchType.GENES, SearchType.TRANSCRIPTS, SearchType.TRANSLATIONS));
	private final List<DataTypeInfo> dataTypes;

	/**
	 * @param provider
	 */
	public GeneSearch(SearchRegistry provider) {
		super(provider);
		dataTypes = new ArrayList<>();
		dataTypes.addAll(provider.getSearch(SearchType.GENES).getDataTypes());
		dataTypes.addAll(provider.getSearch(SearchType.SEQUENCES).getDataTypes());
		dataTypes.addAll(provider.getSearch(SearchType.GENOMES).getDataTypes());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.impl.JoinAwareSearch#isPassThrough(java.lang.
	 * String)
	 */
	@Override
	protected boolean isPassThrough(SearchType type) {
		return type == null || PASSTHROUGH.contains(type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ensembl.genesearch.impl.JoinAwareSearch#getFromJoinFields(org.ensembl
	 * .genesearch.impl.SearchType)
	 */
	@Override
	protected List<String> getFromJoinFields(SearchType type) {
		List<String> fields = new ArrayList<>();
		switch (type) {
		case SEQUENCES:
			fields.add("stable_id");
			fields.add("genome");
			break;
		case HOMOLOGUES:
			fields.add("homologues.stable_id");
			break;
		case GENOMES:
			fields.add("genome");
			break;
		default:
			fields.add("id");
		}
		return fields;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ensembl.genesearch.impl.JoinAwareSearch#getToJoinField(org.ensembl.
	 * genesearch.impl.SearchType)
	 */
	@Override
	protected String getToJoinField(SearchType type) {
		return "id";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.impl.JoinAwareSearch#getDefaultType()
	 */
	@Override
	protected SearchType getDefaultType() {
		return SearchType.GENES;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.impl.JoinAwareSearch#maxJoinSize(org.ensembl.
	 * genesearch.impl.SearchType)
	 */
	@Override
	protected int maxJoinSize(SearchType type) {
		return 100000;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.Search#getDataTypes()
	 */
	@Override
	public List<DataTypeInfo> getDataTypes() {
		return dataTypes;
	}

}
