package org.ensembl.gti.genesearch.services.info;

import java.util.List;

/**
 * Interface for providing {@link DataTypeInfo}
 * 
 * @author dstaines
 *
 */
public interface DataTypeInfoProvider {

	public List<String> getAllNames();

	public List<DataTypeInfo> getAll();

	public DataTypeInfo getByName(String name);

}
