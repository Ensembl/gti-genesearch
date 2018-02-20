package org.ensembl.gti.genesearch.services;

import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.impl.EVAGenomeFinder;
import org.ensembl.genesearch.impl.EVAGenomeRestSearch;
import org.ensembl.genesearch.impl.EVAVariantRestSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.impl.SearchType;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.springframework.beans.factory.annotation.Value;

/**
 * Implementation using EVA variation stored in REST
 * 
 * @author dstaines
 *
 */
public class EVAEndpointProvider extends EndpointSearchProvider {

    @Value("${eva.rest.url:}")
    private String evaRestUrl;

    public EVAEndpointProvider() {
        super();
    }

    @Override
    protected void registerSearches(SearchRegistry reg) {
        super.registerSearches(reg);
        DataTypeInfo evaGenomeType = DataTypeInfo.fromResource("/evagenomes_datatype_info.json");
        DataTypeInfo variantType = DataTypeInfo.fromResource("/evavariants_datatype_info.json");
        Search evaGenomesSearch = new EVAGenomeRestSearch(evaRestUrl, evaGenomeType);
        Search variantSearch = new EVAVariantRestSearch(evaRestUrl, variantType,
                new EVAGenomeFinder(evaGenomesSearch, reg.getSearch(SearchType.GENOMES)));
        reg.registerSearch(SearchType.VARIANTS, variantSearch);
    }

}
