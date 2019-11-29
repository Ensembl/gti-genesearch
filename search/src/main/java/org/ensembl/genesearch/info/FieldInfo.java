/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ensembl.genesearch.info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates information about a field. This information should be given to a
 * client so that it can determine what operations are supported.
 * <p>
 * Typically, this is used to prevent clients from executing unsupported or very
 * expensive operations.
 *
 * @author dstaines
 * @author mchakiachvili
 */
public class FieldInfo {

    private static Logger log = LoggerFactory.getLogger(FieldInfo.class);

    public static FieldInfo clone(String path, FieldInfo info) {
        FieldInfo newInfo = new FieldInfo(path + "." + info.getName(), info.getType());
        log.debug("New Info %s %s ", path + "." + info.getName(), info.getType());
        newInfo.setDisplayName(info.getDisplayName());
        newInfo.setDisplay(info.isDisplay());
        newInfo.setFacet(info.isFacet());
        newInfo.setSearch(info.isSearch());
        newInfo.setSort(info.isSort());
        if (newInfo.type.equals(FieldType.ENUM)) {
            newInfo.setValues(info.getValues());
        }
        return newInfo;
    }

    private boolean display;

    private String displayName;

    private boolean facet;

    private String name;

    private boolean search;

    private boolean sort;

    private String[] values;

    private FieldType type = FieldType.TEXT;

    public FieldInfo(String name, FieldType type) {
        this.name = name;
        this.type = type;
    }

    public FieldInfo() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getName() {
        return name;
    }

    public FieldType getType() {
        return type;
    }

    public boolean isDisplay() {
        return display;
    }

    public boolean isFacet() {
        return facet;
    }

    public boolean isSearch() {
        return search;
    }

    public boolean isSort() {
        return sort;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setFacet(boolean facet) {
        this.facet = facet;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSearch(boolean search) {
        this.search = search;
    }

    public void setSort(boolean sort) {
        this.sort = sort;
    }

    public void setType(FieldType type) {
        this.type = type;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    @Override
    public String toString() {
        return "FieldInfo [display=" + display + ", displayName=" + displayName + ", facet=" + facet + ", name=" + name
                + ", search=" + search + ", sort=" + sort + ", type=" + type + "]";
    }

}
