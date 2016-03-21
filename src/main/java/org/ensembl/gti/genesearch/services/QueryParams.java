package org.ensembl.gti.genesearch.services;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

public class QueryParams extends FetchParams {

	@QueryParam("facets")
	@DefaultValue("")
	private String facets;

	@QueryParam("limit")
	@DefaultValue("10")
	private int limit;

	public String getFacets() {
		return facets;
	}

	public void setFacets(String facets) {
		this.facets = facets;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}