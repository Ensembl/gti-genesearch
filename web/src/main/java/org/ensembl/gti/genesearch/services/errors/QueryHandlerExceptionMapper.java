package org.ensembl.gti.genesearch.services.errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.ensembl.genesearch.query.QueryHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class QueryHandlerExceptionMapper implements ExceptionMapper<QueryHandlerException> {

	Logger log = LoggerFactory.getLogger(this.getClass());

	@Override
	public Response toResponse(QueryHandlerException exception) {
		return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).build();
	}

}
