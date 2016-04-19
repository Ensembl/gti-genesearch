package org.ensembl.gti.genesearch.services.info;

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
	private FieldType type = FieldType.TEXT;
	private boolean search = true;
	private boolean display = true;
	private boolean facet = true;

	public FieldInfo() {
	}

	public FieldInfo(String name, String displayName, FieldType type, boolean search, boolean display, boolean facet) {
		this.name = name;
		this.displayName = displayName;
		this.search = search;
		this.display = display;
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

	public boolean isDisplay() {
		return display;
	}

	public boolean isFacet() {
		return facet;
	}

	public boolean isSearch() {
		return search;
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

	public void setType(FieldType type) {
		this.type = type;
	}

}
