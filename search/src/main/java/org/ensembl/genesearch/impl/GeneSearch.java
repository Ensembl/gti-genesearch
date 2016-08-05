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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author dstaines
 *
 */
public class GeneSearch extends JoinAwareSearch {

	private static final Set<SearchType> PASSTHROUGH = new HashSet<>(
			Arrays.asList(SearchType.GENES, SearchType.TRANSCRIPTS, SearchType.TRANSLATIONS));

	/**
	 * @param provider
	 */
	public GeneSearch(SearchRegistry provider) {
		super(provider);
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

	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.impl.JoinAwareSearch#maxJoinSize(org.ensembl.genesearch.impl.SearchType)
	 */
	@Override
	protected int maxJoinSize(SearchType type) {
		return 100000;
	}

}
