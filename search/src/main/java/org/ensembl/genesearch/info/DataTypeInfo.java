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

package org.ensembl.genesearch.info;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.ensembl.genesearch.impl.SearchType;
import org.ensembl.genesearch.info.FieldInfo.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Object encapsulating information about a data source and its fields
 * 
 * @author dstaines
 *
 */
public class DataTypeInfo {

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final TypeReference<DataTypeInfo> typeRef = new TypeReference<DataTypeInfo>() {
	};

	public static DataTypeInfo fromString(String json) {
		try {
			return mapper.readValue(json, typeRef);
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not parse data type from JSON string", e);
		}
	}

	public static DataTypeInfo fromResource(String resourceName) {
		try {
			return fromString(IOUtils.toString(DataTypeInfo.class.getResource(resourceName), StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not parse data type from resource " + resourceName, e);
		}
	}

	private SearchType name;
	private List<FieldInfo> fieldInfo;
	private List<SearchType> targets;

	public DataTypeInfo() {
	}

	public SearchType getName() {
		return name;
	}

	public void setName(String name) {
		this.name = SearchType.findByName(name);
	}
	

	public void setNameType(SearchType name) {
		this.name = name;
	}

	public List<FieldInfo> getFieldInfo() {
		return fieldInfo;
	}

	public void setFieldInfo(List<FieldInfo> fieldInfo) {
		this.fieldInfo = fieldInfo;
	}

	public List<SearchType> getTargets() {
		return targets;
	}

	public void setTargets(List<String> targets) {
		this.targets = targets.stream().map(SearchType::findByName).collect(Collectors.toList());
	}

	public List<FieldInfo> getFacetableFields() {
		return getFieldInfo().stream().filter(f -> f.isFacet()).collect(Collectors.toList());
	}

	public List<FieldInfo> getDisplayFields() {
		return getFieldInfo().stream().filter(f -> f.isDisplay()).collect(Collectors.toList());
	}

	public List<FieldInfo> getSearchFields() {
		return getFieldInfo().stream().filter(f -> f.isSearch()).collect(Collectors.toList());
	}

	public List<FieldInfo> getSortFields() {
		return getFieldInfo().stream().filter(f -> f.isSort()).collect(Collectors.toList());
	}

	public FieldInfo getFieldByName(String name) {
		return getFieldInfo().stream().filter(f -> name.equalsIgnoreCase(f.getName())).findAny().orElse(null);
	}

	public List<FieldInfo> getFieldByType(FieldType type) {
		return getFieldInfo().stream().filter(f -> type.equals(f.getType())).collect(Collectors.toList());
	}

	/**
	 * Find detailed fieldinfo for the supplied field name, supporting wildcards
	 * 
	 * @param fieldName
	 * @return
	 */
	public List<FieldInfo> getInfoForFieldName(String fieldName) {
		List<FieldInfo> fields = new ArrayList<>(1);
		FieldInfo info = getFieldByName(fieldName);
		if (info != null) {
			fields.add(info);
		} else if (fieldName.indexOf('*') > -1) {
			// not found, try wildcards
			if (fieldName.length() == 1) {
				fields.addAll(this.fieldInfo);
			} else {
				String fieldNameStart = fieldName.substring(0, fieldName.indexOf('*'));
				fields.addAll(this.fieldInfo.stream().filter(f -> f.getName().startsWith(fieldNameStart))
						.collect(Collectors.toList()));
			}
		}
		return fields;
	}

}
