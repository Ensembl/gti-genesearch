package org.ensembl.gti.genesearch.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch;
import org.ensembl.genesearch.query.DefaultQueryHandler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for encapsulating parameters for {@link GeneSearch} services. Can
 * be used as pure POJO or with bindings to query params as strings or ints
 * 
 * @author dstaines
 *
 */

public class FetchParams {

	public static List<String> stringToList(String s) {
		if (StringUtils.isEmpty(s)) {
			return Collections.emptyList();
		} else {
			return Arrays.asList(s.split(","));
		}
	}
	
	private List<String> fields = Collections.emptyList();
	private List<GeneQuery> queries = Collections.emptyList();
	private List<String> sorts = Collections.emptyList();
	
	public List<String> getFields() {
		return fields;
	}

	public List<GeneQuery> getQueries() {
		return queries;
	}

	@JsonProperty("sorts")
	public List<String> getSorts() {
		return sorts;
	}

	@JsonProperty("fields")
	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	@QueryParam("fields")
	@DefaultValue("id,name,genome,description")
	@JsonIgnore
	public void setFields(String fields) {
		this.fields = stringToList(fields);
	}

	@JsonIgnore
	public void setQueries(List<GeneQuery> queries) {
		this.queries = queries;
	}

	@JsonProperty("query")
	public void setQuery(Map<String,Object> query) {
		setQueries(new DefaultQueryHandler().parseQuery(query));
	}

	@QueryParam("query")
	@DefaultValue("")
	@JsonIgnore
	public void setQuery(String query) {
		setQueries(new DefaultQueryHandler().parseQuery(query));
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

	@Override
	public String toString() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}