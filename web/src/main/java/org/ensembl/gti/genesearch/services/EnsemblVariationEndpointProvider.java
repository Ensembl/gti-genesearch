package org.ensembl.gti.genesearch.services;

import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Default implementation using Ensembl variation stored in Elasticsearch
 * 
 * @author dstaines
 *
 */
@Component
@Profile("default")
public class EnsemblVariationEndpointProvider extends EndpointSearchProvider {

    @Value("${es.variants.index:variants}")
    private String variantsIndex = ESSearch.VARIANTS_INDEX;

    public EnsemblVariationEndpointProvider() {
        super();
    }

    @Override
    protected void registerSearches(SearchRegistry reg) {
        super.registerSearches(reg);
        DataTypeInfo variantType = DataTypeInfo.fromResource("/datatypes/es_variants_datatype_info.json");
        Search variantSearch = new ESSearch(getESClient(), variantsIndex, ESSearch.VARIANT_ESTYPE, variantType);
        reg.registerSearch(SearchType.VARIANTS, variantSearch);
    }

}
