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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.ensembl.genesearch.GeneSearch;
import org.ensembl.genesearch.GeneSearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch.GeneQuery.GeneQueryType;
import org.ensembl.genesearch.impl.ESGeneSearch;
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
public class IdLookupClient {

	public static class Params {

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

		@Parameter(names = "-cluster", description = "Cluster to join")
		private String clusterName = "genesearch";

		@Parameter(names = "-host", description = "Host to query")
		private String hostName;

		@Parameter(names = "-port", description = "Port to query")
		private int port = 9300;

		@Parameter(names = "-help", help = true)
		private boolean help;

	}

	private final static Logger log = LoggerFactory
			.getLogger(IdLookupClient.class);

	public static void main(String[] args) throws InterruptedException,
			IOException {

		Params params = new Params();
		JCommander jc = new JCommander(params, args);
		jc.setProgramName(IdLookupClient.class.getSimpleName());
		Node node = null;
		Client client = null;
		if (!isEmpty(params.hostName)) {
			Settings settings = Settings.settingsBuilder()
					.put("cluster.name", params.clusterName).build();
			log.info("Connecting to " + params.hostName);
			client = TransportClient
					.builder()
					.settings(settings)
					.build()
					.addTransportAddress(
							new InetSocketTransportAddress(InetAddress
									.getByName(params.hostName), params.port));
		} else if (!isEmpty(params.clusterName)) {
			// on startup
			log.info("Joining cluster " + params.clusterName);
			node = nodeBuilder().clusterName(params.clusterName).node();
			client = node.client();
		} else {
			jc.usage();
			System.exit(1);
		}

		// do query stuff
		if (client != null) {

			final Writer out = getWriter(params);

			GeneSearch search = new ESGeneSearch(client);

			List<String> ids = params.queryIds;
			if (ids == null && !isEmpty(params.queryFile)) {
				ids = Files.lines(new File(params.queryFile).toPath()).collect(
						Collectors.toList());
			}

			Collection<GeneQuery> queries = Arrays
					.asList(new GeneQuery[] { new GeneQuery(GeneQueryType.TERM,
							params.queryField, ids) });
			search.query(row -> {
				try {
					out.write(StringUtils.join(row.values(), "\t"));
					out.write('\n');
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
				}
			}, queries, params.resultField
					.toArray(new String[params.resultField.size()]));

			log.info("Completed retrieval");
			out.flush();
			out.close();

			log.info("Closing client");
			client.close();
		}
		if (node != null) {
			log.info("Closing node");
			node.close();
		}

	}

	protected static Writer getWriter(Params params) throws IOException {
		Writer out = null;
		if (!isEmpty(params.outFile)) {
			log.info("Writing output to " + params.outFile);
			out = new FileWriter(new File(params.outFile));
		} else {
			out = new OutputStreamWriter(System.out);
		}
		return out;
	}
}
