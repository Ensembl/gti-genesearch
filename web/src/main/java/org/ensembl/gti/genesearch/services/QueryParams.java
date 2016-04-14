package org.ensembl.gti.genesearch.services;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QueryParams extends FetchParams {

	private int limit = 10;
	private int offset = 0;
	private List<String> facets = Collections.emptyList();
	private List<String> sorts = Collections.emptyList();
	private String callback;

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
	
	public int getOffset() {
		return offset;
	}

	@QueryParam("offset")
	@DefaultValue("0")
	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getCallback() {
		return callback;
	}

	@QueryParam("callback")
	@DefaultValue("")
	public void setCallback(String callback) {
		this.callback = callback;
	}

	
	@QueryParam("sorts")
	@DefaultValue("")
	@JsonProperty("sorts")
	public List<String> getSorts() {
		return sorts;
	}

	@JsonProperty("sort")
	public void setSorts(List<String> sorts) {
		this.sorts = sorts;
	}

	@QueryParam("sort")
	@DefaultValue("")
	@JsonIgnore
	public void setSorts(String sort) {
		this.sorts = stringToList(sort);
	}

}