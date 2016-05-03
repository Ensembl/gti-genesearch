package org.ensembl.gti.genesearch.services.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {
	
	Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public Response toResponse(Throwable exception) {
		log.warn("Unexpected exception", exception);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(exception.getMessage()).build();
	}

}
