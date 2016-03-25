package org.ensembl.gti.genesearch.services;

import org.elasticsearch.client.Client;
import org.ensembl.genesearch.GeneSearch;
import org.ensembl.genesearch.clients.ClientBuilder;
import org.ensembl.genesearch.impl.ESGeneSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeneSearchProvider {

	final Logger log = LoggerFactory.getLogger(this.getClass());
	protected GeneSearch search;
	@Value("${es.host}")
	private String hostName;
	@Value("${es.cluster}")
	private String clusterName;
	@Value("${es.port}")
	private int port;
	@Value("${es.node}")
	private boolean node;
	
	public GeneSearchProvider() {
	}
	public GeneSearchProvider(GeneSearch search) {
		this.search = search;
	}

	public GeneSearch getGeneSearch() {
		if(search==null) {
			Client client;
			if (node) {
				log.info("Joining cluster "+this.clusterName+" via "+this.hostName );
				client = ClientBuilder.buildClusterClient(this.clusterName,
						this.hostName);
			} else {
				log.info("Connecting to cluster "+this.clusterName+" on "+this.hostName+":"+this.port );
				client = ClientBuilder.buildTransportClient(this.clusterName,
						this.hostName, this.port);
			}
			search = new ESGeneSearch(client);
		}
		return search;
	}
	
	public void setGeneSearch(GeneSearch search) {
		this.search = search;
	}

}
