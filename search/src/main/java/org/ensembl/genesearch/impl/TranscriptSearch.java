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

import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;

/**
 * Implementation of transcript search that can join to other searches
 * 
 * @author dstaines
 *
 */
public class TranscriptSearch extends JoinMergeSearch {

	/**
	 * @param provider
	 */
	public TranscriptSearch(SearchRegistry provider) {
		super(SearchType.TRANSCRIPTS, provider);
		Search seqSearch = provider.getSearch(SearchType.SEQUENCES);
		if (seqSearch != null) {
			joinTargets.put(SearchType.SEQUENCES, JoinStrategy.as(MergeStrategy.APPEND, "id", "query", "genes.genome"));
		}
		Search variantSearch = provider.getSearch(SearchType.VARIANTS);
		if (variantSearch != null) {
			joinTargets.put(SearchType.VARIANTS, JoinStrategy.as(MergeStrategy.APPEND, "id", "annot.xrefs.id"));
		}
		Search expressionSearch = provider.getSearch(SearchType.EXPRESSION);
		if (expressionSearch != null) {
			joinTargets.put(SearchType.EXPRESSION, JoinStrategy.as(MergeStrategy.APPEND, "id", "bioentityIdentifier"));
		}
	}

}
