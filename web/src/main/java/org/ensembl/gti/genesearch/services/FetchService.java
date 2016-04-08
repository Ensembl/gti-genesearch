package org.ensembl.gti.genesearch.services;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

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
public class FetchService {

	final Logger log = LoggerFactory.getLogger(FetchService.class);
	protected final GeneSearchProvider provider;

	@Autowired
	public FetchService(GeneSearchProvider provider) {
		this.provider = provider;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@BeanParam FetchParams params) {
		return fetch(params);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(@RequestBody FetchParams params) throws JsonParseException, JsonMappingException, IOException {
		return fetch(params);
	}

	public static Response resultsToResponse(List<Map<String, Object>> results) {
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				JsonGenerator jg = new ObjectMapper().getFactory().createGenerator(os, JsonEncoding.UTF8);
				jg.writeStartArray();
				for (Map<String, Object> result : results) {
					jg.writeObject(result);
				}
				jg.writeEndArray();

				jg.flush();
				jg.close();
			}
		};
		return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
	}

	public Response fetch(FetchParams params) {
		log.info("fetch:" + params.toString());		
		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream os) throws IOException, WebApplicationException {
				JsonGenerator jg = new ObjectMapper().getFactory().createGenerator(os, JsonEncoding.UTF8);
				jg.writeStartArray();
				provider.getGeneSearch().fetch(new Consumer<Map<String,Object>>() {
					
					@Override
					public void accept(Map<String, Object> t) {
						try {
							jg.writeObject(t);
						} catch (IOException e) {
							throw new RuntimeException("Could not write fetch results", e);
						}
					}
				}, params.getQueries(), params.getFields(), params.getSorts());
				jg.writeEndArray();

				jg.flush();
				jg.close();
			}
		};
		return Response.ok().entity(stream).type(MediaType.APPLICATION_JSON).build();
	}
}
