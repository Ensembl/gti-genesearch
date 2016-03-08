package org.ensembl.genesearch.clients;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientBuilder {

	private final static Logger log = LoggerFactory
			.getLogger(ClientBuilder.class);

	public static Client buildClient(ClientParams params) {
		Client client = null;
		if (!isEmpty(params.hostName)) {
			client = buildClient(params.clusterName, params.hostName,
					params.port);
		} else if (!isEmpty(params.clusterName)) {
			client = buildClient(params.clusterName);
		}
		return client;
	}

	public static Client buildClient(String clusterName, String hostName,
			int port) {
		Settings settings = Settings.settingsBuilder()
				.put("cluster.name", clusterName).build();
		log.info("Connecting to " + hostName);
		try {
			return TransportClient
					.builder()
					.settings(settings)
					.build()
					.addTransportAddress(
							new InetSocketTransportAddress(InetAddress
									.getByName(hostName), port));
		} catch (UnknownHostException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	public static Client buildClient(String clusterName) {
		// on startup
		log.info("Joining cluster " + clusterName);
		Node node = nodeBuilder().clusterName(clusterName).node();
		// close the node when we're shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			node.close();
		}));
		return node.client();

	}

}
