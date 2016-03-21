package org.ensembl.gti.genesearch.services;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {
	public JerseyConfig() {
		register(HealthService.class);
		register(QueryService.class);
		register(FetchService.class);
		register(GeneService.class);
	}
}