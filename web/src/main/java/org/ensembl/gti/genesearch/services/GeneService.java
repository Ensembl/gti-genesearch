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

import org.ensembl.genesearch.Search;
import org.ensembl.gti.genesearch.services.converter.MapXmlWriter;
import org.ensembl.gti.genesearch.services.errors.GeneNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author dstaines
 *
 */
@Component
@Path("/genes")
public class GeneService extends ObjectService {

	/**
	 * @param provider
	 */
	@Autowired
	public GeneService(SearchProvider provider) {
		super(provider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.gti.genesearch.services.ObjectService#getSearch()
	 */
	@Override
	public Search getSearch() {
		return provider.getGeneSearch();
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
