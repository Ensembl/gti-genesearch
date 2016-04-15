package org.ensembl.gti.genesearch.services.errors;

public class GeneNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public GeneNotFoundException(String message) {
		super(message);
	}
	
}
