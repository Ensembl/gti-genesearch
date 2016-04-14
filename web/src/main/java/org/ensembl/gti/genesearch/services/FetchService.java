package org.ensembl.gti.genesearch.services;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.function.Consumer;

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

import org.ensembl.gti.genesearch.services.converter.MapXmlWriter;
import org.glassfish.jersey.server.JSONP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Path("/fetch")
@Produces({ MediaType.APPLICATION_JSON + ";qs=1", Application.APPLICATION_X_JAVASCRIPT, MediaType.TEXT_PLAIN + ";qs=0.1", MediaType.TEXT_HTML + ";qs=0.1" })
@Consumes(MediaType.APPLICATION_JSON)
public class FetchService {

	final Logger log = LoggerFactory.getLogger(FetchService.class);
	protected final GeneSearchProvider provider;

	@Autowired
	public FetchService(GeneSearchProvider provider) {
		this.provider = provider;
	}

	@GET
	@JSONP
	public Response get(@BeanParam FetchParams params) {
		return fetchAsJson(params);
	}

	@GET
	@JSONP
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	public Response getAsXml(@BeanParam FetchParams params) {
		return fetchAsXml(params);
	}

	@POST
	@JSONP
	public Response post(@RequestBody FetchParams params) throws JsonParseException, JsonMappingException, IOException {
		return fetchAsJson(params);
	}

	@POST
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	@Consumes(MediaType.APPLICATION_XML)
	public Response postAsXml(@RequestBody FetchParams params)
			throws JsonParseException, JsonMappingException, IOException {
		return fetchAsXml(params);
	}

	public Response fetchAsJson(FetchParams params) {
		log.info("fetch to JSON:" + params.toString());
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				JsonGenerator jg = new ObjectMapper().getFactory().createGenerator(os, JsonEncoding.UTF8);
				jg.writeStartArray();
				provider.getGeneSearch().fetch(new Consumer<Map<String, Object>>() {

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

				jg.flush();
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
			public void write(OutputStream os) throws IOException, WebApplicationException {

				try {
					XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
					xsw.writeStartDocument();
					xsw.writeStartElement("genes");
					MapXmlWriter writer = new MapXmlWriter(xsw);
					provider.getGeneSearch().fetch(new Consumer<Map<String, Object>>() {
						@Override
						public void accept(Map<String, Object> t) {
							try {
								writer.writeObject("gene", t);
							} catch (XMLStreamException e) {
								e.printStackTrace();
							}
						}
					}, params.getQueries(), params.getFields());
					xsw.writeEndElement();
					xsw.writeEndDocument();
					xsw.close();
				} catch (XMLStreamException | FactoryConfigurationError e) {
					throw new WebApplicationException(e);
				}
			}
		};
		return Response.ok().entity(stream).type(MediaType.APPLICATION_XML)
				.header("Content-Disposition", "attachment; filename=" + params.getFileName() + ".xml").build();
	}
}
