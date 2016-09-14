package org.ensembl.genesearch.info;

import java.util.List;

import org.ensembl.genesearch.info.FieldInfo.FieldType;

@Deprecated
public interface FieldInfoProvider {

	public List<FieldInfo> getAll();

	public List<FieldInfo> getFacetable();

	public FieldInfo getByName(String name);

	public List<FieldInfo> getByType(FieldType type);

}
