package org.ensembl.gti.genesearch.services;

import org.ensembl.genesearch.impl.CellLineSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.impl.SearchType;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementation using Ensembl variation stored in Elasticsearch
 * 
 * @author dstaines
 *
 */
@Component
@Profile("ebisc")
public class EbiscEndpointProvider extends EndpointSearchProvider {

    @Value("${ebisc.rest.url}")
    private String ebiscUrl;
    @Value("${ebisc.rest.username}")
    private String ebiscUser;
    @Value("${ebisc.rest.api_token}")
    private String ebiscToken;
    @Value("${ebisc.ega.url}")
    private String egaUrl;

    public EbiscEndpointProvider() {
        super();
    }

    @Override
    protected void registerSearches(SearchRegistry reg) {
        super.registerSearches(reg);
        // EBiSC cell line metadata
        DataTypeInfo cellLineType = DataTypeInfo.fromResource("/celllines_datatype_info.json");
        cellLineSearch = new CellLineSearch(cellLineType, ebiscUrl, ebiscUser, ebiscToken);
        reg.registerSearch(SearchType.CELL_LINES, cellLineSearch);
        //TODO HTSget API
        
    }

}
