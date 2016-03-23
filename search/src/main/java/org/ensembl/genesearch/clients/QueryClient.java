package org.ensembl.genesearch.clients;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * A simple standalone CLI client for querying by one field at a time
 * 
 * @author dstaines
 *
 */
public class QueryClient {

	public static class Params extends ClientParams {

		@Parameter(names = "-query", description = "JSON query string")
		private String query = "{}";

		@SuppressWarnings("unchecked")
		@Parameter(names = "-fields", description = "Fields to retrieve")
		private List<String> resultFields = Collections.emptyList();

		@SuppressWarnings("unchecked")
		@Parameter(names = "-facets", description = "Fields to facet by")
		private List<String> facets = Collections.emptyList();

		@SuppressWarnings("unchecked")
		@Parameter(names = "-sort", description = "Fields to sort by")
		private List<String> sorts = Collections.emptyList();

		@Parameter(names = "-limit", description = "Number of rows to retrieve")
		private int limit = 10;

		@Parameter(names = "-outfile", description = "File to write results to")
		private String outFile = null;

	}

	private final static Logger log = LoggerFactory
			.getLogger(QueryClient.class);

	public static void main(String[] args) throws InterruptedException,
			IOException {

		Params params = new Params();
		JCommander jc = new JCommander(params, args);
		jc.setProgramName(QueryClient.class.getSimpleName());
		log.info("Creating client");
		Client client = ClientBuilder.buildClient(params);

		if (client == null) {
			jc.usage();
			System.exit(1);
		}

		GeneSearch search = new ESGeneSearch(client);

		Collection<GeneQuery> queries = new DefaultQueryHandler()
				.parseQuery(params.query);

		log.info("Starting query");

		QueryResult res = search.query(queries, params.resultFields,
				params.facets, params.limit, params.sorts);

		if (!StringUtils.isEmpty(params.outFile)) {
			log.info("Writing results to " + params.outFile);
			FileUtils.write(new File(params.outFile), res.toString());
		}

		log.info("Completed retrieval");

		log.info("Closing client");
		client.close();

	}
}
