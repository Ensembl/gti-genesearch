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
