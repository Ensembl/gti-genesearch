package org.ensembl.gti.genesearch.services.errors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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
		Status status = Response.Status.INTERNAL_SERVER_ERROR;
		return Response.status(status).entity(new ErrorDetails(status, exception)).type(MediaType.APPLICATION_JSON)
				.build();
	}

}
