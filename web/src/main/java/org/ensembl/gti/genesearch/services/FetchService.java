package org.ensembl.gti.genesearch.services;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

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
		return fetchAsResponse(params);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response post(@RequestBody FetchParams params) throws JsonParseException, JsonMappingException, IOException {
		return fetchAsResponse(params);
	}

	public Response fetchAsResponse(FetchParams params) {
		List<Map<String, Object>> results = fetch(params);
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

	public List<Map<String, Object>> fetch(FetchParams params) {
		log.info("fetch:" + params.toString());
		return provider.getGeneSearch().fetch(params.getQueries(), params.getFields(), params.getSorts());
	}

}
