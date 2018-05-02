package org.ensembl.genesearch.impl;

import org.springframework.http.HttpStatus;

/**
 * Unchecked wrapper exception to wrap problems with {@link RestBasedSearch}.
 * Also provides URI and status code in structured form.
 * 
 * @author dstaines
 *
 */
public class RestSearchException extends RuntimeException {

    private final String uri;
    private HttpStatus statusCode;

    public RestSearchException(String uri, String details, HttpStatus statusCode) {
        super(details);
        this.uri = uri;
        this.setStatusCode(statusCode);
    }

    public RestSearchException(String uri, String details, Throwable e) {
        super(details, e);
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatus statusCode) {
        this.statusCode = statusCode;
    }

    private static final long serialVersionUID = 1L;

}
