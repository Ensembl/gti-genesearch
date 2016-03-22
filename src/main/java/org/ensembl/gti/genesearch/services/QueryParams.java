package org.ensembl.gti.genesearch.services;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryParams extends FetchParams {

	private List<String> facets = Collections.emptyList();
	private int limit = 10;

	public List<String> getFacets() {
		return facets;
	}
	
	@JsonProperty("facets")
	public void setFacets(List<String> facets) {
		this.facets = facets;
	}
	
	@QueryParam("facets")
	@DefaultValue("")
	@JsonIgnore
	public void setFacets(String facets) {
		this.facets = stringToList(facets);
	}

	public int getLimit() {
		return limit;
	}

	@QueryParam("limit")
	@DefaultValue("10")
	public void setLimit(int limit) {
		this.limit = limit;
	}

}