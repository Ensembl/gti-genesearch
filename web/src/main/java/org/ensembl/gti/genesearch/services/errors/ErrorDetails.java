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
package org.ensembl.gti.genesearch.services.errors;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

public class ErrorDetails {
	
	public int status;
	public String error;
	public String message;
	public String timeStamp;
	public String trace;
	
	public ErrorDetails(Status status, Throwable e) {
		this.status = status.getStatusCode();
		this.error = e.getClass().getSimpleName();
		this.message = String.valueOf(e.getMessage());
		this.timeStamp =  DateFormat.getDateTimeInstance().format(new Date());
	}
	
	public ErrorDetails(int status, Map<String, Object> errorAttributes) {
		this.status = status;
		this.error = String.valueOf(errorAttributes.get("error"));
		this.message = String.valueOf(errorAttributes.get("message"));
		this.timeStamp = String.valueOf(errorAttributes.get("timestamp").toString());
		this.trace = String.valueOf(errorAttributes.get("trace"));
	}

}