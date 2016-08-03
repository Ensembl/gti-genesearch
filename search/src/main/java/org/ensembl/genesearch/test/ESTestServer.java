/*
 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ensembl.genesearch.test;

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
import org.ensembl.genesearch.impl.ESSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

/**
 * Utility to create and load a test server. Found in main to allow reuse in
 * downstream projects
 * 
 * @author dstaines
 *
 */
public class ESTestServer {

	private final Node node;
	private final Client client;
	private final File dataDir;
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public ESTestServer() {
		dataDir = Files.createTempDir();
		dataDir.deleteOnExit();
		log.info("Starting test server");
		node = new NodeBuilder()
				.settings(Settings.settingsBuilder().put("http.enabled", false).put("path.home", dataDir.getPath()))
				.local(true).node();
		client = node.client();

		try {
			log.info("Reading mapping");
			// slurp the mapping file into memory
			String geneMapping = readResource("/" + ESSearch.GENE_TYPE + "_mapping.json");
			String genomeMapping = readResource("/" + ESSearch.GENOME_TYPE + "_mapping.json");
			log.info("Creating index");

			// create an index with mapping
			client.admin().indices().prepareCreate(ESSearch.GENES_INDEX)
					.setSettings(Settings.builder().put("index.number_of_shards", 4).put("index.number_of_replicas", 0))
					.addMapping(ESSearch.GENE_TYPE, geneMapping).addMapping(ESSearch.GENOME_TYPE, genomeMapping).get();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public Client getClient() {
		return client;
	}

	private final ObjectMapper mapper = new ObjectMapper();

	public void indexTestDocs(String json, String type) {
		try {

			log.info("Indexing ");

			int n = 0;
			if (type.equals("gene")) {
				Map<String, Object> genome = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
				});
				for (Map<String, Object> doc : (List<Map<String, Object>>) genome.get("genes")) {
					indexTestDoc(doc, type);
					n++;
				}
			} else if (type.equals("genome")) {
				List<Map<String, Object>> genomes = mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
				});
				for (Map<String, Object> genome : genomes) {
					indexTestDoc(genome, type);
					n++;
				}
			} else {
				throw new RuntimeException("Don't know how to index " + type);
			}
			// wait for indices to be built
			client.admin().indices().refresh(new RefreshRequest(ESSearch.GENES_INDEX)).actionGet();
			log.info("Indexed " + n + " documents");
			return;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}

	protected void indexTestDoc(Map<String, Object> doc, String type) throws JsonProcessingException {
		String id = String.valueOf(doc.get("id"));
		doc.put("_id", id);

		client.prepareIndex(ESSearch.GENES_INDEX, type).setId(id).setSource(mapper.writeValueAsString(doc)).execute()
				.actionGet();
	}

	public void disconnect() {
		client.close();
		node.close();
	}

	public static String readGzipResource(String name) throws IOException {
		return IOUtils.toString(new GZIPInputStream(ESTestServer.class.getResourceAsStream(name)));
	}

	public static String readResource(String name) throws IOException {
		return IOUtils.toString(ESTestServer.class.getResourceAsStream(name));
	}

}
