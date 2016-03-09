package org.ensembl.genesearch;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

public class ESTestServer {

	private final Node node;
	private final Client client;
	private final File dataDir;
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public ESTestServer() {
		dataDir = Files.createTempDir();
		dataDir.deleteOnExit();
		node = new NodeBuilder()
				.settings(
						Settings.settingsBuilder().put("http.enabled", false)
								.put("path.home", dataDir.getPath()))
				.local(true).node();
		client = node.client();
	}

	public Client getClient() {
		return client;
	}

	public void createTestIndex(String geneJson) {
		try {
			log.info("Starting test server");

			log.info("Reading mapping");
			// slurp the mapping file into memory
			String mapping = readResource("/gene_mapping.json");

			log.info("Creating index");
			// create an index with mapping
			client.admin()
					.indices()
					.prepareCreate(ESGeneSearch.DEFAULT_INDEX)
					.setSettings(
							Settings.builder().put("index.number_of_shards", 4)
									.put("index.number_of_replicas", 0))
					.addMapping(ESGeneSearch.DEFAULT_TYPE, mapping).get();

			ObjectMapper mapper = new ObjectMapper();
			List<Map<String, Object>> genes = mapper.readValue(geneJson,
					new TypeReference<List<Map<String, Object>>>() {
					});
			log.info("Read " + genes.size() + " documents");
			for (Map<String, Object> gene : genes) {

				gene.put("_id", gene.get("id"));

				client.prepareIndex(ESGeneSearch.DEFAULT_INDEX,
						ESGeneSearch.DEFAULT_TYPE)
						.setSource(mapper.writeValueAsString(gene)).execute()
						.actionGet();

			}
			// wait for indices to be built
			client.admin().indices().refresh(new RefreshRequest(ESGeneSearch.DEFAULT_INDEX)).actionGet();
			log.info("Indexed " + genes.size() + " documents");
			return;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}

	public void disconnect() {
		client.close();
		node.close();
	}

	public static String readGzipResource(String name) throws IOException {
		return IOUtils.toString(new GZIPInputStream(ESGeneSearchTest.class
				.getResourceAsStream(name)));
	}

	public static String readResource(String name) throws IOException {
		return IOUtils.toString(ESGeneSearchTest.class
				.getResourceAsStream(name));
	}

}
