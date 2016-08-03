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
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.clients.ClientBuilder;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.impl.GeneSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.impl.SearchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class to allow construction and injection of searches for REST endpoints
 * @author dstaines
 *
 */
@Component
public class EndpointSearchProvider {

	final Logger log = LoggerFactory.getLogger(this.getClass());
	protected Search geneSearch = null;
	protected Search genomeSearch = null;
	protected Client client = null;
	private SearchRegistry registry = null;
	@Value("${es.host}")
	private String hostName;
	@Value("${es.cluster}")
	private String clusterName;
	@Value("${es.port}")
	private int port;
	@Value("${es.node}")
	private boolean node;

	public EndpointSearchProvider() {
	}

	public Client getClient() {
		if (client == null) {
			if (node) {
				log.info("Joining cluster " + this.clusterName + " via " + this.hostName);
				client = ClientBuilder.buildClusterClient(this.clusterName, this.hostName);
			} else {
				log.info("Connecting to cluster " + this.clusterName + " on " + this.hostName + ":" + this.port);
				client = ClientBuilder.buildTransportClient(this.clusterName, this.hostName, this.port);
			}
		}
		return client;
	}
	
	public void setClient(Client client) {
		this.client = client;
	}


	protected SearchRegistry getRegistry() {
		if (registry == null) {
			Search esGenomeSearch = new ESSearch(getClient(), ESSearch.GENES_INDEX, ESSearch.GENOME_TYPE);
			Search esGeneSearch = new ESSearch(getClient(), ESSearch.GENES_INDEX, ESSearch.GENE_TYPE);
			registry = new SearchRegistry().registerSearch(SearchType.GENES, esGeneSearch)
					.registerSearch(SearchType.HOMOLOGUES, esGeneSearch)
					.registerSearch(SearchType.GENOMES, esGenomeSearch);
		}
		return registry;
	}

	public Search getGeneSearch() {
		if (geneSearch == null) {
			geneSearch = new GeneSearch(getRegistry());
		}
		return geneSearch;
	}

	public Search getGenomeSearch() {
		if (genomeSearch == null) {
			genomeSearch = getRegistry().getSearch(SearchType.GENOMES);
		}
		return genomeSearch;
	}

	public void setGeneSearch(Search search) {
		this.geneSearch = search;
	}

	public void setGenomeSearch(Search search) {
		this.genomeSearch = search;
	}

}
