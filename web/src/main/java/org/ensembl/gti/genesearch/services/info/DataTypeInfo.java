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

package org.ensembl.gti.genesearch.services.info;

import java.util.List;
import java.util.stream.Collectors;

import org.ensembl.gti.genesearch.services.info.FieldInfo.FieldType;

/**
 * Object encapsulating information about a data source and its fields
 * 
 * @author dstaines
 *
 */
public class DataTypeInfo {

	private String name;
	private List<FieldInfo> fieldInfo;
	private List<String> targets;

	public DataTypeInfo() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<FieldInfo> getFieldInfo() {
		return fieldInfo;
	}

	public void setFieldInfo(List<FieldInfo> fieldInfo) {
		this.fieldInfo = fieldInfo;
	}

	public List<String> getTargets() {
		return targets;
	}

	public void setTargets(List<String> targets) {
		this.targets = targets;
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
		return getFieldInfo().stream().filter(f -> name.equals(f.getName())).findAny().orElse(null);		
	}

	public List<FieldInfo> getFieldByType(FieldType type) {
		return getFieldInfo().stream().filter(f -> type.equals(f.getType())).collect(Collectors.toList());				
	}

}
