package org.ensembl.gti.genesearch.services;

import java.io.IOException;
import java.io.OutputStream;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Path("/genes")
public class GeneService {

	final Logger log = LoggerFactory.getLogger(GeneService.class);
	protected final GeneSearchProvider provider;

	@Autowired
	public GeneService(GeneSearchProvider provider) {
		this.provider = provider;
	}

	@Path("{id}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> get(@PathParam("id") String id) {
		return provider.getGeneSearch().fetchById(id);
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

}
