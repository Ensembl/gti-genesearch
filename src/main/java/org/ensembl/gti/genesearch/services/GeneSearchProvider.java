package org.ensembl.gti.genesearch.services;

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
	private final GeneSearch search;
	@Value("${es.host}")
	private String hostName = "127.0.0.1";
	@Value("${es.cluster}")
	private String clusterName = "genesearch";
	@Value("${es.port}")
	private int port = 9300;

	public GeneSearchProvider() {
		search = new ESGeneSearch(ClientBuilder.buildTransportClient(
				this.clusterName, this.hostName, this.port));
	}
	
	public GeneSearch getGeneSearch() {
		return search;
	}
	
}
