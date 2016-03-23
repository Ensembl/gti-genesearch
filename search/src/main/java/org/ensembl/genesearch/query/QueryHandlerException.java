package org.ensembl.genesearch.query;

public class QueryHandlerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public QueryHandlerException() {
	}

	public QueryHandlerException(String message) {
		super(message);
	}

	public QueryHandlerException(Throwable cause) {
		super(cause);
	}

	public QueryHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

}
