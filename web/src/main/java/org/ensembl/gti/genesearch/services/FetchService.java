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
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
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
import org.ensembl.genesearch.Search;
import org.ensembl.gti.genesearch.services.converter.MapXmlWriter;
import org.glassfish.jersey.server.JSONP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/fetch")
@Produces({ MediaType.APPLICATION_JSON + ";qs=1", Application.APPLICATION_X_JAVASCRIPT,
		MediaType.TEXT_PLAIN + ";qs=0.1", MediaType.TEXT_HTML + ";qs=0.1" })
@Consumes(MediaType.APPLICATION_JSON)
public abstract class FetchService {

	final Logger log = LoggerFactory.getLogger(FetchService.class);
	protected final SearchProvider provider;

	public FetchService(SearchProvider provider) {
		this.provider = provider;
	}

	public abstract Search getSearch();

	protected abstract String getObjectType();

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
	public Response post(@RequestBody FetchParams params) throws JsonParseException, JsonMappingException, IOException {
		if (StringUtils.isEmpty(params.getAccept())) {
			params.setAccept(MediaType.APPLICATION_JSON);
		}
		return fetchByAccept(params);
	}

	@POST
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	@Consumes(MediaType.APPLICATION_XML)
	public Response postAsXml(@RequestBody FetchParams params)
			throws JsonParseException, JsonMappingException, IOException {
		if (StringUtils.isEmpty(params.getAccept())) {
			params.setAccept(MediaType.APPLICATION_XML);
		}
		return fetchByAccept(params);
	}

	@POST
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	@Consumes({ MediaType.APPLICATION_JSON })
	public Response postAsCsv(@RequestBody FetchParams params)
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
				jg.writeStartArray();
				getSearch().fetch(new Consumer<Map<String, Object>>() {
					@Override
					public void accept(Map<String, Object> t) {
						try {
							jg.writeObject(t);
						} catch (IOException e) {
							throw new WebApplicationException("Could not write fetch results", e);
						}
					}
				}, params.getQueries(), params.getFields());
				jg.writeEndArray();
				jg.close();
			}

		};
		return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON)
				.header("Content-Disposition", "attachment; filename=" + params.getFileName() + ".json").build();
	}

	public Response fetchAsXml(FetchParams params) {
		log.info("fetch to XML:" + params.toString());
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try {
					XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(output);
					xsw.writeStartDocument();
					xsw.writeStartElement(getObjectType() + "s");
					MapXmlWriter writer = new MapXmlWriter(xsw);
					getSearch().fetch(new Consumer<Map<String, Object>>() {
						@Override
						public void accept(Map<String, Object> t) {
							try {
								writer.writeObject(getObjectType(), t);
							} catch (XMLStreamException e) {
								throw new WebApplicationException(e);
							}
						}
					}, params.getQueries(), params.getFields());
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
				writer.write(StringUtils.join(params.getFields(), ','));
				writer.write('\n');
				provider.getGeneSearch().fetch(new Consumer<Map<String, Object>>() {
					@Override
					public void accept(Map<String, Object> t) {
						try {
							writer.write(params.getFields().stream().map(e -> String.valueOf(t.get(e)))
									.collect(Collectors.joining(",")));
							writer.write('\n');
						} catch (IOException e) {
							throw new WebApplicationException(e);
						}
					}
				}, params.getQueries(), params.getFields());
				writer.close();
			}
		};
		return Response.ok().entity(stream).type(Application.TEXT_CSV)
				.header("Content-Disposition", "attachment; filename=" + params.getFileName() + ".csv").build();
	}

}