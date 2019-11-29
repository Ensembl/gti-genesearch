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
package org.ensembl.genesearch.clients;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
        boolean sniff = Boolean.parseBoolean(System.getProperty("es.sniff", "true"));
        Settings settings = Settings.builder().put("cluster.name", params.clusterName)
                .put("client.transport.sniff", sniff)
                .build();
        log.info("Connecting to " + params.hostName + ":" + params.port);
        try {
            client = new PreBuiltTransportClient(settings).
                    addTransportAddress(new TransportAddress(InetAddress.getByName(params.hostName), params.port));
        } catch (UnknownHostException e) {
            throw new ClientBuilderException(e.getMessage(), e);
        }
        return client;
    }

    public static Client buildClient(String clusterName, String hostName, int port) {
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
