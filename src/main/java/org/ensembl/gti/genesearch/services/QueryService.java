package org.ensembl.gti.genesearch.services;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.ensembl.genesearch.GeneSearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/query")
public class QueryService {

	public static class QueryParams {

		@QueryParam("query")
		private String queryField = "_id";

		@QueryParam("terms")
		private String queryIds;

		@QueryParam("fields")
		private String resultField;

	}

	final Logger log = LoggerFactory.getLogger(QueryService.class);
	private final ESGeneSearch search;

	public QueryService() {
		try {
			Settings settings = Settings.settingsBuilder()
					.put("cluster.name", "genesearch").build();
			log.info("Connecting to " + "127.0.0.1");
			Client client = TransportClient
					.builder()
					.settings(settings)
					.build()
					.addTransportAddress(
							new InetSocketTransportAddress(InetAddress
									.getByName("127.0.0.1"), 9300));
			search = new ESGeneSearch(client);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> query(@BeanParam QueryParams params) {
		log.info("queryField:"+params.queryField);
		log.info("queryIds:"+params.queryIds);
		log.info("resultField:"+params.resultField);
		Collection<GeneQuery> queries = Arrays
				.asList(new GeneQuery[] { new GeneQuery(GeneQueryType.TERM,
						params.queryField, params.queryIds.split(",")) });
		return search.query(queries,
				params.resultField.split(","));
	}

}
