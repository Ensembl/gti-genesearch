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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.gti.genesearch.services.converter.MapXmlWriter;
import org.glassfish.jersey.server.JSONP;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base service for /fetch
 * 
 * @author dstaines
 *
 */
@Path("/fetch")
@Produces({ MediaType.APPLICATION_JSON + ";qs=1", Application.APPLICATION_X_JAVASCRIPT,
		MediaType.TEXT_PLAIN + ";qs=0.1", MediaType.TEXT_HTML + ";qs=0.1" })
@Consumes(MediaType.APPLICATION_JSON)
public abstract class FetchService extends SearchBasedService {

	public FetchService(EndpointSearchProvider provider) {
		super(provider);
	}
	
	@GET
	@JSONP
	public Response get(@BeanParam FetchParams params) {
		if (StringUtils.isEmpty(params.getAccept())) {
			params.setAccept(MediaType.APPLICATION_JSON);
		}
		return fetchByAccept(params);
	}

	@GET
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	public Response getAsXml(@BeanParam FetchParams params) {
		if (StringUtils.isEmpty(params.getAccept())) {
			params.setAccept(MediaType.APPLICATION_XML);
		}
		return fetchByAccept(params);
	}

	@GET
	@Produces({ Application.APPLICATION_EXCEL + ";qs=0.1", Application.TEXT_CSV + ";qs=0.1" })
	public Response getAsCsv(@BeanParam FetchParams params) {
		if (StringUtils.isEmpty(params.getAccept())) {
			params.setAccept(Application.APPLICATION_EXCEL);
		}
		return fetchByAccept(params);
	}

	@POST
	@JSONP
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON + ";qs=1", Application.APPLICATION_X_JAVASCRIPT,
			MediaType.TEXT_PLAIN + ";qs=0.1", MediaType.TEXT_HTML + ";qs=0.1" })
	public Response post(@RequestBody FetchParams params) throws JsonParseException, JsonMappingException, IOException {
		if (StringUtils.isEmpty(params.getAccept())) {
			params.setAccept(MediaType.APPLICATION_JSON);
		}
		return fetchByAccept(params);
	}

	@POST
	@Consumes({ MediaType.APPLICATION_FORM_URLENCODED })
	public Response postAsForm(@FormParam("accept") String accept, @FormParam("query") String query,
			@FormParam("fields") String fields) {
		FetchParams params = new FetchParams();
		params.setAccept(accept);
		params.setQuery(query);
		params.setFields(fields);
		return fetchByAccept(params);
	}

	@POST
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	@Consumes(MediaType.APPLICATION_XML)
	public Response postAsXml(@BeanParam @RequestBody FetchParams params)
			throws JsonParseException, JsonMappingException, IOException {
		if (StringUtils.isEmpty(params.getAccept())) {
			params.setAccept(MediaType.APPLICATION_XML);
		}
		return fetchByAccept(params);
	}

	@POST
	@Produces(Application.APPLICATION_EXCEL + ";qs=0.1")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response postAsCsv(@BeanParam @RequestBody FetchParams params)
			throws JsonParseException, JsonMappingException, IOException {
		if (StringUtils.isEmpty(params.getAccept())) {
			params.setAccept(MediaType.APPLICATION_XML);
		}
		return fetchByAccept(params);
	}

	public Response fetchByAccept(FetchParams params) {
		Response response;
		switch (params.getAccept()) {
		case MediaType.APPLICATION_JSON:
		case Application.APPLICATION_X_JAVASCRIPT: {
			response = fetchAsJson(params);
			break;
		}
		case MediaType.APPLICATION_XML: {
			response = fetchAsXml(params);
			break;
		}
		case Application.APPLICATION_EXCEL:
		case Application.TEXT_CSV: {
			response = fetchAsCsv(params);
			break;
		}
		default: {
			throw new WebApplicationException("Cannot provide content of type " + params.getAccept(),
					Response.Status.BAD_REQUEST);
		}
		}
		return response;
	}

	public Response fetchAsJson(FetchParams params) {
		log.info("fetch to JSON:" + params.toString());
		StreamingOutput stream = new StreamingOutput() {

			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				JsonGenerator jg = new ObjectMapper().getFactory().createGenerator(output, JsonEncoding.UTF8);
				jg.writeStartObject();
				List<FieldInfo> fieldInfo = getSearch().getFieldInfo(params.getFields());
				jg.writeObjectField("fields", fieldInfo);
				jg.writeFieldName("results");
				jg.writeStartArray();
				Consumer<Map<String, Object>> consumer = null;
				if (params.isArray()) {
					consumer = t -> {
						try {
							jg.writeStartArray();
							for (FieldInfo f : fieldInfo) {
								jg.writeObject(t.get(f.getName()));
							}
							jg.writeEndArray();
						} catch (IOException e) {
							throw new WebApplicationException("Could not write fetch results", e);
						}
					};

				} else {
					consumer = t -> {
						try {
							jg.writeObject(t);
						} catch (IOException e) {
							throw new WebApplicationException("Could not write fetch results", e);
						}
					};
				}
				getSearch().fetch(consumer, params.getQueries(), params.getFields());
				jg.writeEndArray();
				jg.writeEndObject();
				jg.close();
			}

		};
		return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON)
				.header("Content-Disposition", "attachment; filename=" + params.getFileName() + ".json").build();
	}

	public Response fetchAsXml(FetchParams params) {
		String name = getSearch().getDataType().getName().getObjectName();
		log.info("fetch to XML:" + params.toString());
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try {
					XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
					xsw.writeStartDocument();
					xsw.writeStartElement(name + "s");
					xsw.writeStartElement("fields");
					for (FieldInfo info : getSearch().getFieldInfo(params.getFields())) {
						xsw.writeStartElement("field");
						xsw.writeAttribute("name", info.getName());
						xsw.writeAttribute("displayName", info.getDisplayName());
						xsw.writeAttribute("type", info.getType().toString());
						xsw.writeAttribute("display", Boolean.toString(info.isDisplay()));
						xsw.writeAttribute("facet", Boolean.toString(info.isFacet()));
						xsw.writeAttribute("sort", Boolean.toString(info.isSort()));
						xsw.writeAttribute("search", Boolean.toString(info.isSearch()));
						xsw.writeEndElement();
					}
					xsw.writeEndElement();
					xsw.writeStartElement("results");
					MapXmlWriter writer = new MapXmlWriter(xsw);
					getSearch().fetch(t -> {
							try {
								writer.writeObject(name, t);
							} catch (XMLStreamException e) {
								throw new WebApplicationException(e);
							}
					}, params.getQueries(), params.getFields());
					xsw.writeEndElement();
					xsw.writeEndElement();
					xsw.writeEndDocument();
				} catch (XMLStreamException | FactoryConfigurationError e) {
					throw new WebApplicationException(e);
				}
			}
		};
		return Response.ok().entity(stream).type(MediaType.APPLICATION_XML)
				.header("Content-Disposition", "attachment; filename=" + params.getFileName() + ".xml").build();
	}

	public Response fetchAsCsv(FetchParams params) {
		log.info("fetch to CSV:" + params.toString());
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {

				Writer writer = new OutputStreamWriter(os);
				List<String> fieldNames = getSearch().getFieldInfo(params.getFields()).stream().map(i -> i.getName())
						.collect(Collectors.toList());
				writer.write(StringUtils.join(fieldNames, ','));
				writer.write('\n');
				getSearch().fetch(t -> {
						try {
							writer.write(fieldNames.stream().map(e -> String.valueOf(t.get(e)))
									.collect(Collectors.joining(",")));
							writer.write('\n');
						} catch (IOException e) {
							throw new WebApplicationException(e);
					}
				}, params.getQueries(), params.getFields());
				writer.close();
			}
		};
		return Response.ok().entity(stream).type(Application.TEXT_CSV)
				.header("Content-Disposition", "attachment; filename=" + params.getFileName() + ".csv").build();
	}

}