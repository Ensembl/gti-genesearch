package org.ensembl.gti.genesearch.services;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@Service
@Path("/fetch")
public class FetchService {

	final Logger log = LoggerFactory.getLogger(FetchService.class);
	protected final GeneSearchProvider provider;

	@Autowired
	public FetchService(GeneSearchProvider provider) {
		this.provider = provider;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> get(@BeanParam FetchParams params) {
		return fetch(params);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> post(@RequestBody FetchParams params) throws JsonParseException, JsonMappingException, IOException {
		return fetch(params);
	}

	public List<Map<String, Object>> fetch(FetchParams params) {
		log.info("fetch:" + params.toString());
		return provider.getGeneSearch().fetch(params.getQueries(), params.getFields(), params.getSorts());
	}

}
