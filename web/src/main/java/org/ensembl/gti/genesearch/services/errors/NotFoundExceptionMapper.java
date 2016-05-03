package org.ensembl.gti.genesearch.services.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<GeneNotFoundException> {

	Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public Response toResponse(GeneNotFoundException exception) {
		return Response.status(Response.Status.NOT_FOUND).entity(exception.getMessage()).build();
	}

}
