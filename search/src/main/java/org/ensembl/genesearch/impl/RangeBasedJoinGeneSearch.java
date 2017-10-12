package org.ensembl.genesearch.impl;

import org.ensembl.genesearch.Search;

public class RangeBasedJoinGeneSearch extends GeneSearch {

    public RangeBasedJoinGeneSearch(SearchRegistry provider) {
        super(provider);
        Search variantSearch = provider.getSearch(SearchType.VARIANTS);
        if (variantSearch != null) {
            joinTargets.put(SearchType.VARIANTS, JoinStrategy.asRange(MergeStrategy.APPEND_LIST, ESSearchBuilder.SEQ_REGION_FIELD,
                    ESSearchBuilder.START_FIELD, ESSearchBuilder.END_FIELD, MongoSearchBuilder.LOCATION_FIELD));
        }
    }

}
