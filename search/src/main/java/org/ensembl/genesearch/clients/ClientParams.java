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
