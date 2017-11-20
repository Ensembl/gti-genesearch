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

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.utils.DataUtils;
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
        createIndex(ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        createIndex(ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
        createIndex(ESSearch.VARIANTS_INDEX, ESSearch.VARIANT_ESTYPE);
    }

    protected void createIndex(String index, String type) {
        try {
            log.info("Reading gene mapping");
            // slurp the mapping file into memory
            String geneMapping = DataUtils.readResource("/" + type + "_index.json");
            geneMapping = geneMapping.replaceAll("SHARDN", "1");
            Map<String, Object> geneIndexObj = mapper.readValue(geneMapping, new TypeReference<Map<String, Object>>() {
            });
            if (client.admin().indices().prepareExists(index).execute().actionGet().isExists()) {
                log.info("Deleting index");
                client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
            }
            log.info("Creating index");
            // create an index with mapping
            Map<String, Object> mappingObj = (Map<String, Object>) geneIndexObj.get("mappings");
            client.admin().indices().prepareCreate(index)
                    .setSettings(mapper.writeValueAsString(geneIndexObj.get("settings")))
                    .addMapping(type, mapper.writeValueAsString(mappingObj.get(type))).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Client getClient() {
        return client;
    }

    private final ObjectMapper mapper = new ObjectMapper();

    public void indexTestDocs(String json, String index, String type) {
        try {

            log.info("Indexing ");

            int n = 0;
            List<Map<String, Object>> docs = mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
            for (Map<String, Object> doc : docs) {
                indexTestDoc(doc, index, type);
                n++;
            }
            // wait for indices to be built
            client.admin().indices().refresh(new RefreshRequest(index)).actionGet();
            log.info("Indexed " + n + " documents");
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
        }
    }

    protected void indexTestDoc(Map<String, Object> doc, String index, String type) throws JsonProcessingException {
        String id = String.valueOf(doc.get("id"));
        client.prepareIndex(index, type).setId(id).setSource(mapper.writeValueAsString(doc)).execute().actionGet();
    }

    public void disconnect() {
        client.close();
        node.close();
    }

}
