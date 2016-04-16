package org.ensembl.gti.genesearch.services;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import org.ensembl.gti.genesearch.services.errors.GeneNotFoundException;
import org.glassfish.jersey.server.JSONP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Path("/genes")
@Produces({ MediaType.APPLICATION_JSON + ";qs=1", Application.APPLICATION_X_JAVASCRIPT,
		MediaType.TEXT_PLAIN + ";qs=0.1", MediaType.TEXT_HTML + ";qs=0.1" })
@Consumes(MediaType.APPLICATION_JSON)
public class GeneService {

	final Logger log = LoggerFactory.getLogger(GeneService.class);
	protected final GeneSearchProvider provider;

	@Autowired
	public GeneService(GeneSearchProvider provider) {
		this.provider = provider;
	}

	@Path("{id}")
	@GET
	@JSONP
	public Map<String, Object> get(@PathParam("id") String id) {
		Map<String, Object> gene = provider.getGeneSearch().fetchById(id);
		if (gene.isEmpty()) {
			throw new GeneNotFoundException("Gene with ID " + id + " not found");
		} else {
			return gene;
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@JSONP
	public Response post(List<String> ids) {

		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				JsonGenerator jg = new ObjectMapper().getFactory().createGenerator(os, JsonEncoding.UTF8);
				jg.writeStartArray();
				provider.getGeneSearch().fetchByIds(new Consumer<Map<String, Object>>() {

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

	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_XML + ";qs=0.1")
	public Response getAsXml(@PathParam("id") String id) {
		try {
			Map<String, Object> gene = provider.getGeneSearch().fetchById(id);
			if (gene.isEmpty()) {
				throw new GeneNotFoundException("Gene with ID " + id + " not found");
			} else {
				String xml = MapXmlWriter.mapToXml("gene", gene);
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

		log.info("genes to XML");
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {

				try {
					XMLStreamWriter xsw = XMLOutputFactory.newInstance().createXMLStreamWriter(os);
					xsw.writeStartDocument();
					xsw.writeStartElement("genes");
					MapXmlWriter writer = new MapXmlWriter(xsw);
					provider.getGeneSearch().fetchByIds(new Consumer<Map<String, Object>>() {
						@Override
						public void accept(Map<String, Object> t) {
							try {
								writer.writeObject("gene", t);
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
				.header("Content-Disposition", "attachment; filename=genes.xml").build();

	}

}
