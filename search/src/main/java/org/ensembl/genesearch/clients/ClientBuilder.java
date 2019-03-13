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

package org.ensembl.genesearch.clients;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for creating an Elastic client from specification in an
 * instance of {@link ClientParams}
 *
 * @author dstaines
 */
public class ClientBuilder {

    public static class ClientBuilderException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ClientBuilderException(String message, Throwable cause) {
            super(message, cause);
        }

        public ClientBuilderException(String message) {
            super(message);
        }

        public ClientBuilderException(Throwable cause) {
            super(cause);
        }

    }

    private static final Logger log = LoggerFactory.getLogger(ClientBuilder.class);

    private ClientBuilder() {
    }

    public static Client buildClient(ClientParams params) {
        Client client = null;
        if (params.joinCluster) {
            // client = buildClusterClient(params.clusterName, params.hostName);
        } else if (!isEmpty(params.clusterName)) {
            // client = buildTransportClient(params.clusterName, params.hostName, params.port);
        }
        return client;
    }

    public static Client buildClusterClient(String clusterName, String hostName) {
        // on startup
        log.info("Joining cluster " + clusterName);
        try {

            Path tempDir = Files.createTempDirectory("esgenesearch_");

            Settings settings = Settings.builder().put("http.enabled", "false").put("transport.tcp.port", "9300-9400")
                    .put("discovery.zen.ping.multicast.enabled", "false")
                    .put("discovery.zen.ping.unicast.hosts", hostName).put("path.home", tempDir.toString()).build();
            Node node = new Node(settings) {

                @Override
                protected void registerDerivedNodeNameWithLogger(String nodeName) {

                }
            };
            return node.client();
            /*
            log.debug(settings.toDelimitedString(','));

            Node node = nodeBuilder().data(false).client(true).clusterName(clusterName).settings(settings).build()
                    .start();
            Node node = new Node(settings) {
                @Override
                protected void registerDerivedNodeNameWithLogger(String nodeName) {

                }
            };
            // close the node when we're shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    node.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            return node.client();
             */
        } catch (IOException e) {
            throw new ClientBuilderException(e.getMessage(), e);
        }


    }

    public static Client buildTransportClient(String clusterName, String hostName, int port) {
        boolean sniff = Boolean.parseBoolean(System.getProperty("es.sniff", "true"));
        Settings settings = Settings.builder().put("cluster.name", clusterName)
                .put("client.transport.sniff", sniff)
                .build();
        log.info("Connecting to " + hostName + ":" + port);
        try {
            return new PreBuiltTransportClient(settings).
                    addTransportAddress(new TransportAddress(InetAddress.getByName(hostName), port));
        } catch (UnknownHostException e) {
            throw new ClientBuilderException(e.getMessage(), e);
        }

    }

}
