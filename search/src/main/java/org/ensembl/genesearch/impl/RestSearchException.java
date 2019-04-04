/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
