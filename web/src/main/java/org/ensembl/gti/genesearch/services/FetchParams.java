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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.Search;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for encapsulating parameters for {@link Search} services. Can be
 * used as pure POJO or with bindings to query params as strings or ints. Note:
 * Cannot be used with form params
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

	private String accept;
	private String contentType;
	private QueryOutput fields = new QueryOutput();
	private String fileName = "genes";
	private Map<String, Object> queries = Collections.emptyMap();
	private boolean array = false;

	public String getAccept() {
		return accept;
	}

	public String getContentType() {
		return contentType;
	}

	public QueryOutput getFields() {
		return fields;
	}

	public String getFileName() {
		return fileName;
	}

	public Map<String, Object> getQueries() {
		return queries;
	}

	@QueryParam("accept")
	public void setAccept(String accept) {
		this.accept = accept;
	}

	@QueryParam("content-type")
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@JsonProperty("fields")
	public void setFields(Object fields) {
		Class<?> clazz = fields.getClass();
		if (List.class.isAssignableFrom(clazz)) {
			this.fields = QueryOutput.build((List<String>) fields);
		} else if (Map.class.isAssignableFrom(fields.getClass())) {
			this.fields = QueryOutput.build((Map<String, Object>) fields);
		} else {
			throw new IllegalArgumentException("Cannot handle query output of class " + clazz.getName());
		}
	}

	@QueryParam("fields")
	@DefaultValue("id,name,genome,description")
	@JsonIgnore
	public void setFields(String fields) {
		if (fields.charAt(0) == '[') {
			this.fields = QueryOutput.build(fields);
		} else {
			this.fields = QueryOutput.build(stringToList(fields));
		}
	}

	@QueryParam("filename")
	@DefaultValue("genes")
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@JsonProperty("query")
	public void setQueries(Map<String, Object> queries) {
		this.queries = queries;
	}

	@QueryParam("query")
	@DefaultValue("")
	@JsonIgnore
	public void setQuery(String query) {
		try {
			if (!StringUtils.isEmpty(query)) {
				setQueries(new ObjectMapper().readValue(query, new TypeReference<Map<String, Object>>() {
				}));
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not parse query " + query, e);
		}
	}

	@Override
	public String toString() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public boolean isArray() {
		return array;
	}

	@QueryParam("array")
	@DefaultValue("false")
	public void setArray(String array) {
		this.array = Boolean.valueOf(array);
	}

	@JsonProperty("array")
	public void setArray(boolean array) {
		this.array = array;
	}

}