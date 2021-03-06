/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ensembl.gti.genesearch.services;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.gti.genesearch.services.converter.MapXmlWriter;
import org.ensembl.gti.genesearch.services.errors.ObjectNotFoundException;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base service for returning whole objects by ID
 * 
 * @author dstaines
 *
 */
public abstract class ObjectService extends SearchBasedService {

	/**
	 * @param provider
	 */
	public ObjectService(EndpointSearchProvider provider, String name) {
		super(provider);
	}

	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> get(@PathParam("id") String id, @QueryParam("fields") String fields) {
		if (StringUtils.isEmpty(fields)) {
			return getSearch().fetchById(id);
		} else {
			return getSearch().fetchById(QueryOutput.build(Arrays.asList(StringUtils.split(fields, ","))), id);
		}
	}

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
	
	
	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	public Response getAsXml(@PathParam("id") String id) {
		String name = getSearch().getDataType().getName().getObjectName();
		try {
			Map<String, Object> object = getSearch().fetchById(id);
			if (object.isEmpty()) {
				throw new ObjectNotFoundException("Object with ID " + id + " not found");
			} else {
				String xml = MapXmlWriter.mapToXml(name, object);
				return Response.ok().entity(xml).type(MediaType.APPLICATION_XML)
						.header("Content-Disposition", "attachment; filename=" + id + ".xml").build();
			}
		} catch (UnsupportedEncodingException | XMLStreamException | FactoryConfigurationError e) {
			throw new WebApplicationException(e);
		}

	}

	@POST
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	@Consumes(MediaType.APPLICATION_XML)
	public Response postAsXml(List<String> ids) {
		String name = getSearch().getDataType().getName().getObjectName();
		String plName = getSearch().getDataType().getName().toString();

		log.info(plName+" to XML");
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {

				try {
					XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
					xsw.writeStartDocument();
					xsw.writeStartElement(plName);
					MapXmlWriter writer = new MapXmlWriter(xsw);
					getSearch().fetchByIds(new Consumer<Map<String, Object>>() {
						@Override
						public void accept(Map<String, Object> t) {
							try {
								writer.writeData(name, t);
							} catch (XMLStreamException e) {
								e.printStackTrace();
							}
						}

					}, ids.toArray(new String[ids.size()]));
					xsw.writeEndElement();
					xsw.writeEndDocument();
					xsw.close();
				} catch (XMLStreamException | FactoryConfigurationError e) {
					throw new WebApplicationException(e);
				}
			}
		};

		return Response.ok().entity(stream).type(MediaType.APPLICATION_XML)
				.header("Content-Disposition", "attachment; filename="+name+"s.xml").build();

	}


}
