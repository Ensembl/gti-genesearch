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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author dstaines
 *
 */
public abstract class ObjectService {

	final Logger log = LoggerFactory.getLogger(ObjectService.class);
	protected final SearchProvider provider;

	public ObjectService(SearchProvider provider) {
		this.provider = provider;
	}

	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> get(@PathParam("id") String id, @QueryParam("fields") String fields) {
		if(StringUtils.isEmpty(fields)) {
			return getSearch().fetchById(id);			
		} else {
			return getSearch().fetchById(Arrays.asList(StringUtils.split(fields,",")),id);
		}
	}

	public abstract Search getSearch();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(List<String> ids) {

		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				JsonGenerator jg = new ObjectMapper().getFactory().createGenerator(os, JsonEncoding.UTF8);
				jg.writeStartArray();
				getSearch().fetchByIds(new Consumer<Map<String, Object>>() {

					@Override
					public void accept(Map<String, Object> t) {
						try {
							jg.writeObject(t);
						} catch (IOException e) {
							throw new RuntimeException("Could not write fetch results", e);
						}
					}
				}, ids.toArray(new String[ids.size()]));
				jg.writeEndArray();

				jg.flush();
				jg.close();
			}
		};
		return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();

	}

	@Path("/select")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public QueryResult get(@QueryParam("query") String query, @DefaultValue("0") @QueryParam("offset") int offset,
			@DefaultValue("10") @QueryParam("limit") int limit) {
		return getSearch().select(query, offset, limit);
	}

}
