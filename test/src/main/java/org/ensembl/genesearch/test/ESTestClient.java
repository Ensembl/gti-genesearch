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
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.ConnectTransportException;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Utility to create and load an in-memory Elastic test server. Note that this
 * is included in the main source folder to allow reuse in downstream projects
 * e.g. REST server.
 *
 * @author dstaines
 */
public class ESTestClient {

    private static Client client;
    private static Logger log = LoggerFactory.getLogger(ESTestClient.class);
    private static ElasticsearchContainer container;

    public ESTestClient() throws RuntimeException {
        /**
         *
         * REMOVED embedded cluster - prerequisites to test : either env var pointing to actual ES node or a local ES node from docker
         * @see
         */
        TransportAddress transportAddress;
        Settings settings;
        String elasticHost = System.getenv("ES_HOST") == null ? "localhost" : System.getenv("ES_HOST");
        String clusterName = System.getenv("ES_CLUSTER_NAME") == null ? "docker-cluster" : System.getenv("ES_CLUSTER_NAME");
        String port = System.getenv("ES_PORT") == null ? "9300" : System.getenv("ES_PORT");
        log.info(String.format("Connection to ES %s:%s - %s ", elasticHost, port, clusterName));
        try {
            transportAddress = new TransportAddress(InetAddress.getByName(elasticHost), Integer.parseInt(port));
            settings = Settings.builder().put("cluster.name", clusterName).build();
            client = new PreBuiltTransportClient(settings).addTransportAddress(transportAddress);
            client.admin().cluster().prepareHealth().setTimeout(TimeValue.timeValueMinutes(5)).execute().actionGet();
            log.info(String.format("Connected to ES %s test server", clusterName));
        } catch (UnknownHostException | ConnectTransportException | NoNodeAvailableException e) {
            log.info("ES server error: " + e.getMessage());
            log.info("Create one with docker");
            container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:6.8.1");
            container.start();
            settings = Settings.builder().put("cluster.name", "docker-cluster").build();
            transportAddress = new TransportAddress(container.getTcpHost());
            client = new PreBuiltTransportClient(settings).addTransportAddress(transportAddress);
            client.admin().cluster().prepareHealth().setTimeout(TimeValue.timeValueMinutes(5)).execute().actionGet();
            log.info(String.format("Connected to embedded ES %s test server", clusterName));
        }
    }

    /**
     * Read a mapping file and create an index. Resource is of the form
     * /{type}_index.json
     *
     * @param index name of index to create
     * @param type  mapping file type
     */
    public void createIndex(String index, String type) {
        try {
            log.info("Reading " + index + " mapping");
            // slurp the mapping file into memory
            String geneMapping = IOUtils.toString(ESTestClient.class.getResourceAsStream("/indexes/" + type + "_index.json"), Charset.defaultCharset());
            geneMapping = geneMapping.replaceAll("SHARDN", "5");
            geneMapping = geneMapping.replaceAll("REPLICAS", "0");
            Map<String, Object> elasticIndexObj = mapper.readValue(geneMapping,
                    new TypeReference<Map<String, Object>>() {
                    }
            );

            if (client.admin().indices().prepareExists(index).execute().actionGet().isExists()) {
                log.info("Index already exists...");
                if (! Boolean.parseBoolean(System.getProperty("keep_index", "false"))) {
                    log.info("Resetting...");
                    client.admin().indices().delete(new DeleteIndexRequest(index)).actionGet();
                    log.info("Index cleaned up");
                }
            } else {
                log.info("Index doesn't exists...\nCreating...");
            }
            if (! Boolean.parseBoolean(System.getProperty("keep_index", "false"))) {
                // only recreate mapping if index has been reset
                Map<String, Object> mappingObj = (Map<String, Object>) elasticIndexObj.get("mappings");
                client.admin().indices().prepareCreate(index).setSettings((Map<String, Object>) elasticIndexObj.get("settings")).get();
                client.admin().indices().preparePutMapping(index).setType(type).setSource(mapper.writeValueAsString(mappingObj.get(type)), XContentType.JSON).get();
                log.info("Mapping created");
            }


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

            log.info("Indexing [Index:" + index + "][Type:" + type + "]");

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
        } catch (IOException e) {
            throw new RuntimeException(e);
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
    protected void indexTestDoc(Map<String, Object> doc, String index, String type) throws
            JsonProcessingException {
        String id = String.valueOf(doc.get("id"));
        log.info("Id used " + id);
        client.prepareIndex(index, type, id).setSource(mapper.writeValueAsString(doc), XContentType.JSON).execute().actionGet();
    }

    /**
     * Close the client and shut down the ES node.
     */
    public void disconnect() {
        if (client != null) client.close();
        if (container != null) container.close();
    }

    public boolean hasContainer() {
        return (container != null);
    }

    public boolean hasClient() {
        return (client != null);
    }
}
