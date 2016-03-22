package org.ensembl.gti.genesearch.services;

import java.util.List;
import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ensembl.genesearch.GeneSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/fetch")
public class FetchService {

	final Logger log = LoggerFactory.getLogger(FetchService.class);
	protected final GeneSearch search;

	@Autowired
	public FetchService(GeneSearchProvider provider) {
		this.search = provider.getGeneSearch();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> get(@BeanParam FetchParams params) {
		return fetch(params);
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> post(@BeanParam FetchParams params, String entity) {
		return fetch(params);
	}
	
	public List<Map<String, Object>> fetch(FetchParams params) {
		log.info("fetch:" + params.toString());
		return search.fetch(params.getQueries(), params.getFields(),
				params.getSorts());
	}

}
