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
public class GeneSearch extends JoinMergeSearch {

	public GeneSearch(SearchRegistry provider) {
		super(SearchType.GENES, provider);
		Search homologSearch = provider.getSearch(SearchType.HOMOLOGUES);
		if (homologSearch != null) {
			joinTargets.put(SearchType.HOMOLOGUES, JoinStrategy.as(MergeStrategy.MERGE, "homologues.stable_id", "id"));
		}
		Search seqSearch = provider.getSearch(SearchType.SEQUENCES);
		if (seqSearch != null) {
			joinTargets.put(SearchType.SEQUENCES, JoinStrategy.as(MergeStrategy.MERGE, "id", "id", "genome"));
		}
		Search genomeSearch = provider.getSearch(SearchType.GENOMES);
		if (genomeSearch != null) {
			joinTargets.put(SearchType.GENOMES, JoinStrategy.as(MergeStrategy.APPEND, "genome", "id"));
		}
		Search variantSearch = provider.getSearch(SearchType.VARIANTS);
		if (variantSearch != null) {
			joinTargets.put(SearchType.VARIANTS, JoinStrategy.as(MergeStrategy.APPEND, "id", "annot.xrefs.id"));
		}
	}

}
