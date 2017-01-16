package org.ensembl.genesearch.info;

/**
 * Encapsulates information about a field
 * 
 * @author dstaines
 *
 */
public class FieldInfo {

	private boolean display;

	private String displayName;

	private boolean facet;

	private String name;

	private boolean search;

	private boolean sort;

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

	@Override
	public String toString() {
		return "FieldInfo [display=" + display + ", displayName=" + displayName + ", facet=" + facet + ", name=" + name
				+ ", search=" + search + ", sort=" + sort + ", type=" + type + "]";
	}
	
}
