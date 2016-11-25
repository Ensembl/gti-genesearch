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

import org.ensembl.genesearch.Search;

/**
 * Implementation of gene search that can join to other searches
 * 
 * @author dstaines
 *
 */
public class VariantSearch extends JoinMergeSearch {

	public VariantSearch(SearchRegistry provider) {
		super(SearchType.VARIANTS, provider);
		Search geneSearch = provider.getSearch(SearchType.GENES);
		if (geneSearch != null) {
			joinTargets.put(SearchType.GENES, JoinStrategy.as(MergeStrategy.APPEND, "annot.ct.ensg", "id"));
		}
		Search transcriptSearch = provider.getSearch(SearchType.TRANSCRIPTS);
		if (transcriptSearch != null) {
			joinTargets.put(SearchType.TRANSCRIPTS, JoinStrategy.as(MergeStrategy.APPEND, "annot.ct.enst", "id"));
		}
	}

}
