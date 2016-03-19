package org.ensembl.gti.genesearch.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch.QuerySort;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.clients.ClientBuilder;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/query")
public class QueryService {

	public static class QueryParams {

		@QueryParam("query")
		@DefaultValue("_id")
		private String queryString;

		@QueryParam("facets")
		@DefaultValue("")
		private String facets;

		@QueryParam("fields")
		@DefaultValue("genome,name,description")
		private String fields;

		@QueryParam("sort")
		@DefaultValue("")
		private String sort;

		@QueryParam("limit")
		@DefaultValue("10")
		private int limit;

		@QueryParam("sortDir")
		@DefaultValue("ASC")
		private String sortDir;

	}

	final Logger log = LoggerFactory.getLogger(QueryService.class);
	private final ESGeneSearch search;
	// @Value("#{es_host}")
	private String hostName = "127.0.0.1";
	// @Value("#{es_cluster}")
	private String clusterName = "genesearch";
	// @Value("#{es_port}")
	private int port = 9300;

	public QueryService() {
		search = new ESGeneSearch(ClientBuilder.buildTransportClient(
				this.clusterName, this.hostName, this.port));
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public QueryResult query(@BeanParam QueryParams params) {
		log.info("query:|" + params.queryString + "|");
		log.info("fields:|" + params.fields + "|");
		log.info("facets:|" + params.facets + "|");
		log.info("sorts:|" + params.sort + "|");

		Collection<GeneQuery> queries = new DefaultQueryHandler()
				.parseQuery(params.queryString);

		List<QuerySort> sorts = stringToList(params.sort)
				.stream()
				.map(sort -> new QuerySort(sort, QuerySort.SortDirection
						.valueOf(params.sortDir.toUpperCase())))
				.collect(Collectors.toList());

		return search.query(queries, stringToList(params.fields),
				stringToList(params.facets), params.limit, sorts);
	}

	private static List<String> stringToList(String s) {
		if(StringUtils.isEmpty(s)) {
			return Collections.EMPTY_LIST;
		} else {
		return Arrays.asList(s.split(","));
		}
		}

}
