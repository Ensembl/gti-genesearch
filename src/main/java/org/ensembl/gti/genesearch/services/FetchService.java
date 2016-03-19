package org.ensembl.gti.genesearch.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.clients.ClientBuilder;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Path("/fetch")
public class FetchService {

	public static class QueryParams {

		@QueryParam("query")@DefaultValue("_id")
		private String queryField = "_id";

		@QueryParam("terms")
		private String queryIds;

		@QueryParam("fields")@DefaultValue("id")
		private String resultField = "id";

	}

	final Logger log = LoggerFactory.getLogger(FetchService.class);
	private final ESGeneSearch search;
	@Value("${es.host}")
	private String hostName = "127.0.0.1";
	@Value("${es.cluster}")
	private String clusterName = "genesearch";
	@Value("${es.port}")
	private int port = 9300;

	public FetchService() {
		search = new ESGeneSearch(ClientBuilder.buildTransportClient(
				this.clusterName, this.hostName, this.port));
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> fetch(@BeanParam QueryParams params) {
		log.info("queryField:" + params.queryField);
		log.info("queryIds:" + params.queryIds);
		log.info("resultField:" + params.resultField);
		Collection<GeneQuery> queries = Arrays
				.asList(new GeneQuery[] { new GeneQuery(GeneQueryType.TERM,
						params.queryField, params.queryIds.split(",")) });
		return search.query(queries, params.resultField.split(","));
	}

}
