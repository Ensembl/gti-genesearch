package org.ensembl.gti.genesearch.services;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.ensembl.genesearch.GeneSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Path("/genes")
public class GeneService {

	final Logger log = LoggerFactory.getLogger(GeneService.class);
	protected final GeneSearch search;

	@Autowired
	public GeneService(GeneSearchProvider provider) {
		this.search = provider.getGeneSearch();
	}

	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> get(@PathParam("id") String id) {
		return search.fetchById(id);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(List<String> ids) {
		return FetchService.resultsToResponse(search.fetchByIds(ids.toArray(new String[ids.size()])));
	}

}
