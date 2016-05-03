/*
 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ensembl.gti.genesearch.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for encapsulating parameters for {@link Search} services. Can
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
	
	private String fileName = "genes";
	private List<String> fields = Collections.emptyList();
	private List<Query> queries = Collections.emptyList();
	
	public String getFileName() {
		return fileName;
	}
	
	public List<String> getFields() {
		return fields;
	}

	public List<Query> getQueries() {
		return queries;
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
	public void setQueries(List<Query> queries) {
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
	
	@QueryParam("filename")
	@DefaultValue("genes")
	public void setFileName(String fileName) {
		this.fileName = fileName;
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