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
        DataTypeInfo variantType = DataTypeInfo.fromResource("/datatypes/variants_datatype_info.json");
        Search variantSearch = new EnsemblVariantSearch(ensRestUrl, variantType);
        reg.registerSearch(SearchType.VARIANTS, variantSearch);
    }

}
