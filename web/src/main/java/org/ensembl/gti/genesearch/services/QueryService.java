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

import java.util.Map;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ensembl.genesearch.QueryResult;
import org.glassfish.jersey.server.JSONP;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Base class for a /query endpoint
 * 
 * @author dstaines
 *
 */
@Produces({ MediaType.APPLICATION_JSON, Application.APPLICATION_X_JAVASCRIPT })
public abstract class QueryService extends SearchBasedService {

	public QueryService(EndpointSearchProvider provider) {
		super(provider);
	}

	@GET
	@JSONP
	public Map<String, Object> get(@BeanParam QueryParams params) {
		log.info("Get from query");
		return query(params);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@JSONP
	public Map<String, Object> post(@RequestBody QueryParams params) {
		log.info("Post to query");
		return query(params);
	}

	public Map<String, Object> query(QueryParams params) {
		log.info("query:" + params);
		QueryResult results = getSearch().query(params.getQueries(), params.getFields(), params.getFacets(),
				params.getOffset(), params.getLimit(), params.getSorts());
		return results.toMap(params.isArray());
	}

}
