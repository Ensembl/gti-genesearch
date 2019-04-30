/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ensembl.genesearch.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.utils.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * Utility to create and load an in-memory Elastic test server. Note that this
 * is included in the main source folder to allow reuse in downstream projects
 * e.g. REST server.
 *
 * @author dstaines
 */
public class ESTestServer {

    private final Client client;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static ElasticsearchContainer container;

    public ESTestServer() {
        TransportClient transportClient = null;
        boolean createIndex = false;
        try {
            log.info("Try to connect to existing test ES");
            // look for a accessible Test ES server available locally
            transportClient = new PreBuiltTransportClient(
                    Settings.builder().put("cluster.name", "genesearch").build())
                    .addTransportAddress(new TransportAddress(InetAddress.getByName("gti-es-test.ebi.ac.uk"), 9300));
            log.info("Connected to ES Docker test server ");
        } catch (UnknownHostException e) {
            log.info("gti-es-test not found");
            container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:6.6.1");
            container.start();
            TransportAddress transportAddress = new TransportAddress(container.getTcpHost());
            Settings settings = Settings.builder().put("cluster.name", "docker-cluster").build();
            transportClient = new PreBuiltTransportClient(settings).addTransportAddress(transportAddress);
            createIndex = true;
        }
        client = transportClient;
        if (createIndex) {
            // only create index when using docker, local test server should be up and set up already
            createIndex(ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
            createIndex(ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
            createIndex(ESSearch.VARIANTS_INDEX, ESSearch.VARIANT_ESTYPE);
            log.debug("Created indexes ");
        }
    }

    public ESTestServer(Client testCaseClient) {
        client = testCaseClient;
        createIndex(ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE);
        createIndex(ESSearch.GENOMES_INDEX, ESSearch.GENOME_ESTYPE);
        createIndex(ESSearch.VARIANTS_INDEX, ESSearch.VARIANT_ESTYPE);
        // TODO add Regulation INDEX
    }

    /**
     * Read a mapping file and create an index. Resource is of the form
     * /{type}_index.json
     *
     * @param index name of index to create
     * @param type  mapping file type
     */
    protected void createIndex(String index, String type) {
        try {
            log.info("Reading " + index + " mapping");
            // slurp the mapping file into memory
            String geneMapping = DataUtils.readResource("/indexes/" + type + "_index.json");
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
            client.admin().indices().prepareCreate(index).setSettings((Map<String, Object>) geneIndexObj.get("settings")).get();
            client.admin().indices().preparePutMapping(index).setType(type).setSource(mapper.writeValueAsString(mappingObj.get(type)), XContentType.JSON).get();
            log.info("Index created");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Client getClient() {
        return client;
    }

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Index the supplied JSON document into the specified index as the
     * specified type
     *
     * @param json
     * @param index
     * @param type
     */
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

    /**
     * Index the supplied object into the specified index as the specified type
     *
     * @param doc
     * @param index
     * @param type
     * @throws JsonProcessingException
     */
    protected void indexTestDoc(Map<String, Object> doc, String index, String type) throws JsonProcessingException {
        String id = String.valueOf(doc.get("id"));
        client.prepareIndex(index, type, id).setSource(mapper.writeValueAsString(doc), XContentType.JSON).get();
    }

    /**
     * Close the client and shut down the ES node.
     */
    public void disconnect() {
        if (client != null)
            client.close();
        if (container != null)
            container.close();
    }

}
