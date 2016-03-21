package org.ensembl.gti.genesearch.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.GeneSearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for encapsulating parameters for {@link GeneSearch} services
 * @author dstaines
 *
 */
public class FetchParams {

	@QueryParam("query")
	@DefaultValue("")
	private String queryString;

	@QueryParam("fields")
	@DefaultValue("genome,name,description")
	private String fields;

	@QueryParam("sort")
	@DefaultValue("")
	private String sort;

	@QueryParam("sortDir")
	@DefaultValue("ASC")
	private String sortDir;

	public static List<String> stringToList(String s) {
		if (StringUtils.isEmpty(s)) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(s.split(","));
		}
	}

	
	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	public String getFields() {
		return fields;
	}

	public void setFields(String fields) {
		this.fields = fields;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getSortDir() {
		return sortDir;
	}

	public void setSortDir(String sortDir) {
		this.sortDir = sortDir;
	}
	
	@Override
	public String toString() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage(),e);
		}
	}

}