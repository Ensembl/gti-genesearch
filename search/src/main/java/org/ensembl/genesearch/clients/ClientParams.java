package org.ensembl.genesearch.clients;

import com.beust.jcommander.Parameter;

public class ClientParams {

	@Parameter(names = "-cluster", description = "Cluster to join")
	protected String clusterName = "genesearch";

	@Parameter(names = "-host", description = "Host to query")
	protected String hostName;

	@Parameter(names = "-port", description = "Port to query")
	protected int port = 9300;
	
	@Parameter(names = "-join_cluster", description = "Whether to join cluster")
	protected boolean joinCluster = false;	

	@Parameter(names = "-help", help = true)
	protected boolean help;

}
