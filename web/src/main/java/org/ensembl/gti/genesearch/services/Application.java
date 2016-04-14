package org.ensembl.gti.genesearch.services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
	
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	public static final String APPLICATION_X_JAVASCRIPT = "application/x-javascript";
	public static final String TEXT_CSV = "text/csv";
	public static final String APPLICATION_EXCEL = "application/vnd.ms-excel";
	
}
