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
 * Implementation of expression search that can join to gene and transcript
 * 
 * @author dstaines
 *
 */
public class ExpressionSearch extends JoinMergeSearch {

	public ExpressionSearch(SearchRegistry provider) {
		super(SearchType.EXPRESSION_ANALYTICS, provider);
		Search geneSearch = provider.getSearch(SearchType.GENES);
		if (geneSearch != null) {
			joinTargets.put(SearchType.GENES, JoinStrategy.as(MergeStrategy.APPEND, "bioentity_identifier", "id"));
		}
		Search transcriptSearch = provider.getSearch(SearchType.TRANSCRIPTS);
		if (transcriptSearch != null) {
			joinTargets.put(SearchType.TRANSCRIPTS, JoinStrategy.as(MergeStrategy.APPEND, "bioentity_identifier", "id"));
		}
		Search experimentsSearch = provider.getSearch(SearchType.EXPRESSION_EXPERIMENTS);
		if (experimentsSearch != null) {
			joinTargets.put(SearchType.EXPRESSION_EXPERIMENTS,
					JoinStrategy.as(MergeStrategy.APPEND, "experiment_accession", "experiment_accession"));
		}

	}

}
