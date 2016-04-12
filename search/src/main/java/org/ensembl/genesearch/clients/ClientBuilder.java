package org.ensembl.genesearch.clients;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			client = buildClusterClient(params.clusterName, params.hostName);
		} else if (!isEmpty(params.clusterName)) {
			client = buildTransportClient(params.clusterName, params.hostName, params.port);
		}
		return client;
	}

	public static Client buildClusterClient(String clusterName, String hostName) {
		// on startup
		log.info("Joining cluster " + clusterName);
		try {

			Path tempDir = Files.createTempDirectory("esgenesearch_");
			
			Settings settings = Settings.builder()
					.put("http.enabled", "false")
					.put("transport.tcp.port", "9300-9400")
					.put("discovery.zen.ping.multicast.enabled", "false")
					.put("discovery.zen.ping.unicast.hosts", hostName)
					.put("path.home",tempDir.toString()).build();
			
			log.debug(settings.toDelimitedString(','));
			
			Node node = nodeBuilder().data(false).client(true).clusterName(clusterName).settings(settings).build()
					.start();
			// close the node when we're shutdown
			Runtime.getRuntime().addShutdownHook(new Thread(() -> node.close()));
			return node.client();
		} catch (IOException e) {
			throw new ClientBuilderException(e.getMessage(), e);
		}

	}

	public static Client buildTransportClient(String clusterName, String hostName, int port) {
		boolean sniff = Boolean.parseBoolean(System.getProperty("es.sniff", "true"));
		Settings settings = Settings.settingsBuilder().put("cluster.name", clusterName)
				.put("client.transport.sniff", sniff).build();
		log.info("Connecting to " + hostName + ":" + port);
		try {
			return TransportClient.builder().settings(settings).build()
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostName), port));
		} catch (UnknownHostException e) {
			throw new ClientBuilderException(e.getMessage(), e);
		}

	}

}
