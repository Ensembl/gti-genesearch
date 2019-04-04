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

import org.ensembl.gti.genesearch.services.errors.DefaultExceptionMapper;
import org.ensembl.gti.genesearch.services.errors.NotFoundExceptionMapper;
import org.ensembl.gti.genesearch.services.errors.QueryHandlerExceptionMapper;
import org.ensembl.gti.genesearch.services.filters.CORSFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.springframework.context.annotation.Configuration;

/**
 * @author dstaines
 *
 */
@Configuration
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        register(HealthService.class);
        register(SwaggerService.class);
        register(GeneQueryService.class);
        register(GeneFetchService.class);
        register(GeneService.class);
        register(GeneInfoService.class);
        register(TranscriptQueryService.class);
        register(TranscriptFetchService.class);
        register(TranscriptService.class);
        register(TranscriptInfoService.class);
        register(GenomeQueryService.class);
        register(GenomeFetchService.class);
        register(GenomeService.class);
        register(GenomeInfoService.class);
        register(VariantQueryService.class);
        register(VariantFetchService.class);
        register(VariantService.class);
        register(VariantInfoService.class);
        register(ExpressionQueryService.class);
        register(ExpressionFetchService.class);
        register(ExpressionService.class);
        register(ExpressionInfoService.class);
        register(CellLineQueryService.class);
        register(CellLineFetchService.class);
        register(CellLineService.class);
        register(CellLineInfoService.class);
        register(JacksonFeature.class);
        register(InfoService.class);
        register(CORSFilter.class);
        register(LoggingFilter.class);
        register(QueryHandlerExceptionMapper.class);
        register(NotFoundExceptionMapper.class);
        register(DefaultExceptionMapper.class);
        EncodingFilter.enableFor(this, GZipEncoder.class);
    }

}