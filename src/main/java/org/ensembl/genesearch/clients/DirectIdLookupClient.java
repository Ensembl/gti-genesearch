package org.ensembl.genesearch.clients;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
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
public class DirectIdLookupClient {

	private static final int SCROLL_SIZE = 1000;
	private static final int TIMEOUT = 600000;

	public static class Params extends ClientParams {

		@Parameter(names = "-query", description = "Field to query")
		private String queryField = "_id";

		@Parameter(names = "-terms", description = "Terms to search on")
		private List<String> queryIds;

		@Parameter(names = "-query_file", description = "File of terms to search on")
		private String queryFile;

		@Parameter(names = "-out_file", description = "File to write results to")
		private String outFile;

		@Parameter(names = "-fields", description = "Fields to retrieve")
		private List<String> resultField = Arrays
				.asList(new String[] { "genome" });

		@Parameter(names = "-source", description = "Retrieve source")
		private boolean source = false;

	}

	private final static String INDEX = "genes";

	public static void main(String[] args) throws InterruptedException,
			IOException {

		final Logger log = LoggerFactory.getLogger(IdLookupClient.class);

		Params params = new Params();
		JCommander jc = new JCommander(params, args);
		jc.setProgramName(IdLookupClient.class.getSimpleName());
		Client client = ClientBuilder.buildClient(params);

		if (client == null) {
			jc.usage();
			System.exit(1);
		}

		// do query stuff
		if (client == null) {
			jc.usage();
			System.exit(1);
		}

		Writer out = null;
		if (!isEmpty(params.outFile)) {
			log.info("Writing output to " + params.outFile);
			out = new FileWriter(new File(params.outFile));
		} else {
			out = new OutputStreamWriter(System.out);
		}

		String[] fields = params.resultField
				.toArray(new String[params.resultField.size()]);

		QueryBuilder query = null;
		if (params.queryIds != null && params.queryIds.size() > 0) {
			if (params.queryField.equals("_id")) {
				query = QueryBuilders.idsQuery("gene").addIds(params.queryIds);
			} else {
				query = QueryBuilders.termsQuery(params.queryField,
						params.queryIds);
			}
		} else if (!isEmpty(params.queryFile)) {
			List<String> ids = Files.lines(new File(params.queryFile).toPath())
					.collect(Collectors.toList());
			if (params.queryField.equals("_id")) {
				query = QueryBuilders.idsQuery("gene").addIds(ids);
			} else {
				query = QueryBuilders.termsQuery(params.queryField, ids);
			}
		} else {
			query = QueryBuilders.matchAllQuery();
		}

		log.info("Starting query");

		SearchRequestBuilder request = client.prepareSearch(INDEX)
				.setPostFilter(query).setFetchSource(params.source)
				.addFields(fields).setSearchType(SearchType.SCAN)
				.setScroll(new TimeValue(TIMEOUT)).setSize(SCROLL_SIZE);
		SearchResponse response = request.execute().actionGet();
		log.info("Retrieved " + response.getHits().totalHits() + " in "
				+ response.getTookInMillis() + " ms");

		// Scroll until no hits are returned
		while (true) {

			for (SearchHit hit : response.getHits().getHits()) {
				if (params.source) {
					out.write(hit.getSourceAsString());
				} else {
					out.write(hit.getId());
					for (String fieldName : params.resultField) {
						SearchHitField field = hit.field(fieldName);
						String val = null;
						if (field != null && field.getValues() != null) {
							val = StringUtils.join(field.getValues().toArray(),
									",");
						}
						out.write("\t" + val);
					}
					out.write("\n");
				}
			}
			// next scroll response
			response = client.prepareSearchScroll(response.getScrollId())
					.setScroll(new TimeValue(60000)).execute().actionGet();
			// Break condition: No hits are returned
			if (response.getHits().getHits().length == 0) {
				break;
			}
		}
		log.info("Completed retrieval");
		out.flush();
		out.close();

		log.info("Closing client");
		client.close();

	}
}
