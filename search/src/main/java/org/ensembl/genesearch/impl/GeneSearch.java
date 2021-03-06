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
 * Implementation of gene search that can join to other searches
 * 
 * @author dstaines
 *
 */
public class GeneSearch extends JoinMergeSearch {

    public GeneSearch(SearchRegistry registry) {
        super(SearchType.GENES, registry);
        Search homologSearch = registry.getSearch(SearchType.HOMOLOGUES);
        if (homologSearch != null) {
            joinTargets.put(SearchType.HOMOLOGUES, JoinStrategy.as(MergeStrategy.MERGE, "homologues.stable_id", "id"));
        }
        Search seqSearch = registry.getSearch(SearchType.SEQUENCES);
        if (seqSearch != null) {
            joinTargets.put(SearchType.SEQUENCES, JoinStrategy.as(MergeStrategy.APPEND, "id", "query", "genome"));
        }
        Search genomeSearch = registry.getSearch(SearchType.GENOMES);
        if (genomeSearch != null) {
            joinTargets.put(SearchType.GENOMES, JoinStrategy.as(MergeStrategy.APPEND, "genome", "id"));
        }
        Search variantSearch = registry.getSearch(SearchType.VARIANTS);
        if (variantSearch != null) {
            // different implementations of 
            if (MongoSearch.class.isAssignableFrom(variantSearch.getClass())) {
                joinTargets.put(SearchType.VARIANTS, JoinStrategy.as(MergeStrategy.APPEND, "id", "annot.xrefs.id"));
            } else if (ESSearch.class.isAssignableFrom(variantSearch.getClass())) {
                joinTargets.put(SearchType.VARIANTS,
                        JoinStrategy.as(MergeStrategy.APPEND, "transcripts.id", "locations.annotations.stable_id"));
            } else if (EVAVariantRestSearch.class.isAssignableFrom(variantSearch.getClass())) {
                joinTargets.put(SearchType.VARIANTS,
                        JoinStrategy.asGenomeRange(MergeStrategy.APPEND, ESSearchBuilder.GENOME_FIELD,
                                ESSearchBuilder.SEQ_REGION_FIELD, ESSearchBuilder.START_FIELD,
                                ESSearchBuilder.END_FIELD, EVAVariantRestSearch.GENOME_FIELD,
                                EVAVariantRestSearch.LOCATION_FIELD));
            } else if (EnsemblVariantSearch.class.isAssignableFrom(variantSearch.getClass())) {
                joinTargets.put(SearchType.VARIANTS,
                        JoinStrategy.asGenomeRange(MergeStrategy.APPEND, ESSearchBuilder.GENOME_FIELD,
                                ESSearchBuilder.SEQ_REGION_FIELD, ESSearchBuilder.START_FIELD,
                                ESSearchBuilder.END_FIELD, EnsemblVariantSearch.GENOME_FIELD,
                                EnsemblVariantSearch.LOCATION_FIELD));
            } else if (HtsGetVariantSearch.class.isAssignableFrom(variantSearch.getClass())) {
                joinTargets.put(SearchType.VARIANTS,
                        JoinStrategy.asRange(MergeStrategy.APPEND, 
                                ESSearchBuilder.SEQ_REGION_FIELD, ESSearchBuilder.START_FIELD,
                                ESSearchBuilder.END_FIELD, 
                                HtsGetVariantSearch.HtsGetArgs.LOCATION));
            } else {
                throw new UnsupportedOperationException(
                        "Cannot use variant search of type " + variantSearch.getClass());
            }
        }
        Search expressionSearch = registry.getSearch(SearchType.EXPRESSION);
        if (expressionSearch != null) {
            joinTargets.put(SearchType.EXPRESSION, JoinStrategy.as(MergeStrategy.APPEND, "id", "bioentity_identifier"));
        }
    }

}
