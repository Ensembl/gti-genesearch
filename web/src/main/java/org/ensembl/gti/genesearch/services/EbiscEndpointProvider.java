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
package org.ensembl.gti.genesearch.services;

import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.impl.CellLineSearch;
import org.ensembl.genesearch.impl.EbiscVariantSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Implementation using EBiSC variation stored in EGA accessed by htsget
 * 
 * @author dstaines
 *
 */
@Component
@Profile("ebisc")
public class EbiscEndpointProvider extends EndpointSearchProvider {

    @Value("${ebisc.rest.url:}")
    private String ebiscUrl;
    @Value("${ebisc.rest.username:}")
    private String ebiscUser;
    @Value("${ebisc.rest.api_token:}")
    private String ebiscToken;
    @Value("${ebisc.ega.url:}")
    private String egaUrl;
    @Value("${ebisc.ega.accession:}")
    private String egaAccession;

    public EbiscEndpointProvider() {
        super();
    }

    @Override
    protected void registerSearches(SearchRegistry reg) {
        super.registerSearches(reg);
        // EBiSC cell line metadata
        DataTypeInfo cellLineType = DataTypeInfo.fromResource("/datatypes/celllines_datatype_info.json");
        CellLineSearch cellLineSearch = new CellLineSearch(cellLineType, ebiscUrl, ebiscUser, ebiscToken);
        reg.registerSearch(SearchType.CELL_LINES, cellLineSearch);
        // HTSget API
        DataTypeInfo variantInfo = DataTypeInfo.fromResource("/datatypes/ebisc_datatype_info.json");
        EbiscVariantSearch variantSearch = new EbiscVariantSearch(variantInfo, egaUrl, egaAccession, cellLineSearch);
        reg.registerSearch(SearchType.VARIANTS, variantSearch);
        this.setCellLineSearch(cellLineSearch);
        this.setVariantSearch(variantSearch);

    }

}
