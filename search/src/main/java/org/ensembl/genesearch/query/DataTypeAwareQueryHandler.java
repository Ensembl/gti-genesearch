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

package org.ensembl.genesearch.query;

import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.FieldType;

/**
 * {@link QueryHandler} that uses an instance of {@link DataTypeInfo} to find
 * out what type a field is, rather than guessing
 * 
 * @author dstaines
 *
 */
public class DataTypeAwareQueryHandler extends DefaultQueryHandler {

	private final DataTypeInfo info;

	/**
	 * @param info
	 *            data type to use
	 */
	public DataTypeAwareQueryHandler(DataTypeInfo info) {
		this.info = info;
	}

	/* (non-Javadoc)
	 * @see org.ensembl.genesearch.query.DefaultQueryHandler#getFieldType(java.lang.String, java.lang.Object)
	 */
	@Override
	protected FieldType getFieldType(String key, Object value) {
		FieldInfo field = info.getFieldByName(key);
		if(field!=null) {
			return field.getType();
		} else {
			log.warn("Could not find field "+key+" from type "+info.getName()+" - guessing type");
			return super.getFieldType(key, value);
		} 
	}

}
