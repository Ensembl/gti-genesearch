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

package org.ensembl.gti.genesearch.services;

import org.ensembl.gti.genesearch.services.errors.DefaultExceptionMapper;
import org.ensembl.gti.genesearch.services.errors.NotFoundExceptionMapper;
import org.ensembl.gti.genesearch.services.errors.QueryHandlerExceptionMapper;
import org.ensembl.gti.genesearch.services.filters.CORSFilter;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

/**
 * @author dstaines
 *
 */
@Configuration
public class JerseyConfig extends ResourceConfig {
	public JerseyConfig() {
		register(HealthService.class);
		register(GeneQueryService.class);
		register(GenomeQueryService.class);
		register(GeneFetchService.class);
		register(GeneService.class);
		register(GenomeFetchService.class);
		register(GenomeService.class);
		register(GeneInfoService.class);
		register(GenomeInfoService.class);
		register(JacksonFeature.class);
		register(InfoService.class);
		register(CORSFilter.class);
		register(LoggingFilter.class);
		register(QueryHandlerExceptionMapper.class);
		register(NotFoundExceptionMapper.class);
		register(DefaultExceptionMapper.class);
	}
}