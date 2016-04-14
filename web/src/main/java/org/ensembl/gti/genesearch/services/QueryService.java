package org.ensembl.gti.genesearch.services;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ensembl.genesearch.QueryResult;
import org.glassfish.jersey.server.JSONP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@Path("/query")
@Produces({ MediaType.APPLICATION_JSON, Application.APPLICATION_X_JAVASCRIPT })
public class QueryService {

	final Logger log = LoggerFactory.getLogger(QueryService.class);
	protected final GeneSearchProvider provider;

	@Autowired
	public QueryService(GeneSearchProvider provider) {
		this.provider = provider;
	}

	@GET
	@JSONP
	public QueryResult get(@BeanParam QueryParams params) {
		log.info("Get from query");
		return query(params);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@JSONP
	public QueryResult post(@RequestBody QueryParams params) {
		log.info("Post to query");
		return query(params);
	}

	public QueryResult query(QueryParams params) {
		log.info("query:" + params);
		return provider.getGeneSearch().query(params.getQueries(), params.getFields(), params.getFacets(),
				params.getOffset(), params.getLimit(), params.getSorts());
	}

}
