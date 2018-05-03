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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Service;

/**
 * Endpoint to check if services are running
 * @author dstaines
 *
 */
@Path("/health")
@Service
public class HealthService {

	final Logger log = LoggerFactory.getLogger(this.getClass());
	protected final EndpointSearchProvider provider;

	/**
	 * @param provider
	 *            provider for retrieving search
	 */
	@Autowired
	public HealthService(EndpointSearchProvider provider) {
		this.provider = provider;
	}

	@GET
	@Produces("application/json")
	public Health health() {
		Health.Builder builder = new Health.Builder();
		boolean ok = true;
		for (SearchType type : SearchType.values()) {
			Search search = provider.getRegistry().getSearch(type);
			if (search != null) {
				if (search.up()) {
					builder.withDetail(type.name(), "up");
				} else {
					builder.withDetail(type.name(), "down");
					ok = false;
				}
			} else {
				log.warn("No search found for type" + type);
			}
		}

		if (ok) {
			builder.up();
		} else {
			builder.down();
		}

		return builder.build();
	}

}
