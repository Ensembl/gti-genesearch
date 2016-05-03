package org.ensembl.gti.genesearch.services.errors;

import java.util.Map;

public class ErrorDetails {

	public int status;
	public String error;
	public String message;
	public String timeStamp;
	public String trace;

	public ErrorDetails(int status, Map<String, Object> errorAttributes) {
		this.status = status;
		this.error = String.valueOf(errorAttributes.get("error"));
		this.message = String.valueOf(errorAttributes.get("message"));
		this.timeStamp = String.valueOf(errorAttributes.get("timestamp").toString());
		this.trace = String.valueOf(errorAttributes.get("trace"));
	}

}