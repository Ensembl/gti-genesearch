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
package org.ensembl.genesearch.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.info.DataTypeInfo;

public class HtsGetSingleFileVariantSearch extends HtsGetVariantSearch {

	private final String[] fileAccessions;
	
	public HtsGetSingleFileVariantSearch(DataTypeInfo type, String baseUrl, String fileAccession) {
		super(type, baseUrl, StringUtils.EMPTY);
		this.fileAccessions = new String[] {fileAccession};
	}

	@Override
	protected HtsGetArgs queryToArgs(List<Query> queries) {
		HtsGetArgs args = super.queryToArgs(queries);
		args.session = null;
		args.datasets = null;
		args.files = fileAccessions;
		return args;
	}
	
	

}
