package org.ensembl.gti.genesearch.services;

import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.impl.EnsemblVariantSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementation using Ensembl variation stored in Elasticsearch
 * 
 * @author dstaines
 *
 */
@Component
@Profile("ensembl_rest")
public class EnsemblRESTVariationEndpointProvider extends EndpointSearchProvider {

    public EnsemblRESTVariationEndpointProvider() {
        super();
    }

    @Override
    protected void registerSearches(SearchRegistry reg) {
        super.registerSearches(reg);
        DataTypeInfo variantType = DataTypeInfo.fromResource("/variants_datatype_info.json");
        Search variantSearch = new EnsemblVariantSearch(ensRestUrl, variantType);
        reg.registerSearch(SearchType.VARIANTS, variantSearch);
    }

}
