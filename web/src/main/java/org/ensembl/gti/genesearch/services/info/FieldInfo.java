package org.ensembl.gti.genesearch.services.info;

import org.apache.commons.lang3.StringUtils;

/**
 * Encapsulates information about a field
 * 
 * @author dstaines
 *
 */
public class FieldInfo {

	public static enum FieldType {
		TEXT, NUMBER, LOCATION, ONTOLOGY;
	}

	private String name;
	private String displayName;
	private String searchField;
	private String displayField;
	private FieldType type = FieldType.TEXT;
	private boolean facet = false;

	public FieldInfo() {
	}

	public FieldInfo(String name, String displayName, String displayField, String searchField, FieldType type,
			boolean facet) {
		this.name = name;
		this.displayName = displayName;
		if (StringUtils.isEmpty(searchField)) {
			searchField = name;
		}
		this.searchField = searchField;
		if (StringUtils.isEmpty(displayField)) {
			displayField = searchField;
		}
		this.displayField = displayField;
		this.facet = facet;
		this.type = type;
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

	public boolean isFacet() {
		return facet;
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

	public void setType(FieldType type) {
		this.type = type;
	}

	public String getSearchField() {
		if (StringUtils.isEmpty(searchField)) {
			return getName();
		} else {
			return searchField;
		}
	}

	public void setSearchField(String searchField) {
		this.searchField = searchField;
	}

	public String getDisplayField() {
		if (StringUtils.isEmpty(displayField)) {
			return getSearchField();
		} else {
			return displayField;
		}
	}

	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

}
