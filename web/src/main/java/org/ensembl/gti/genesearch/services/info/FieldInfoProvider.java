package org.ensembl.gti.genesearch.services.info;

import java.util.List;

import org.ensembl.gti.genesearch.services.info.FieldInfo.FieldType;

public interface FieldInfoProvider {

	public List<FieldInfo> getAll();

	public List<FieldInfo> getFacetable();

	public FieldInfo getByName(String name);

	public List<FieldInfo> getByType(FieldType type);

}
