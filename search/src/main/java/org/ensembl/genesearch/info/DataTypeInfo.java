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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Object encapsulating information about a data source and its fields.
 * Generally populated from static resource files in JSON format.
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
    private List<FieldInfo> fieldInfo = new ArrayList<>();
    private List<SearchType> targets = new ArrayList<>();
    private transient Map<String, FieldInfo> fieldsByNames;

    public DataTypeInfo() {
    }

    public DataTypeInfo(SearchType name) {
        this.name = name;
    }

    private Map<String, FieldInfo> getFieldsByName() {
        if (fieldsByNames == null) {
            fieldsByNames = getFieldInfo().stream().collect(Collectors.toMap(FieldInfo::getName, Function.identity()));
        }
        return fieldsByNames;
    }

    /**
     * @return name of search e.g. gene
     */
    public SearchType getName() {
        return name;
    }

    /**
     * @param name name as string (must be in {@link SearchType}
     */
    public void setName(String name) {
        this.name = SearchType.findByName(name);
    }

    /**
     * @param name
     */
    public void setNameType(SearchType name) {
        this.name = name;
    }

    /**
     * @return all fields for type
     */
    public List<FieldInfo> getFieldInfo() {
        return fieldInfo;
    }

    public void setFieldInfo(List<FieldInfo> fieldInfo) {
        this.fieldInfo = fieldInfo;
    }

    /**
     * @return search types which can be set as join targets for this search
     */
    public List<SearchType> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets.stream().map(SearchType::findByName).collect(Collectors.toList());
    }

    /**
     * @return fields which support facetting
     */
    public List<FieldInfo> getFacetableFields() {
        return getFieldInfo().stream().filter(FieldInfo::isFacet).collect(Collectors.toList());
    }

    /**
     * @return fields which can be displayed
     */
    public List<FieldInfo> getDisplayFields() {
        return getFieldInfo().stream().filter(FieldInfo::isDisplay).collect(Collectors.toList());
    }

    /**
     * @return fields which can be searched
     */
    public List<FieldInfo> getSearchFields() {
        return getFieldInfo().stream().filter(FieldInfo::isSearch).collect(Collectors.toList());
    }

    /**
     * @return fields which can be sorted
     */
    public List<FieldInfo> getSortFields() {
        return getFieldInfo().stream().filter(FieldInfo::isSort).collect(Collectors.toList());
    }

    public FieldInfo getFieldByName(String name) {
        return getFieldsByName().get(name);
    }

    public List<FieldInfo> getFieldByType(FieldType type) {
        return getFieldInfo().stream().filter(f -> type.equals(f.getType())).collect(Collectors.toList());
    }

    /**
     * Find detailed fieldinfo for the supplied field name, supporting wildcards
     * 
     * @param fieldName
     * @return list of field info
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

    /**
     * @return field set with type ID
     */
    public Optional<FieldInfo> getIdField() {
        return getFieldByType(FieldType.ID).stream().findFirst();
    }

    /**
     * fluent method for building a list of fields
     * 
     * @param info
     * @return info being worked on
     */
    public DataTypeInfo addField(FieldInfo info) {
        this.getFieldInfo().add(info);
        return this;
    }

    /**
     * fluent method for building a list of fields
     * 
     * @param name
     * @param type
     * @return info being worked on
     */
    public DataTypeInfo addField(String name, FieldType type) {
        return this.addField(new FieldInfo(name, type));
    }

}
