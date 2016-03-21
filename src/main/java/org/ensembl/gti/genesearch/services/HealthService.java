package org.ensembl.gti.genesearch.services;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Service;

@Path("/health")
@Service
public class HealthService {

	@GET
	@Produces("application/json")
	public Health health() {
		return Health.status("genesearch up and running").build();
	}

}
