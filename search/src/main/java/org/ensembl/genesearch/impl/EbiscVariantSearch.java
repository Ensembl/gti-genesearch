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
import java.util.Map;
import java.util.Optional;

import org.ensembl.genesearch.info.DataTypeInfo;

/**
 * hts-get based search that uses a secondary search for decoration of results
 * with cell line metadata
 * 
 * @author dstaines
 * 
 */
public class EbiscVariantSearch extends HtsGetSingleFileVariantSearch {

	private static final String GENOTYPE_ID = "id";
	private static final String GENOTYPES = "genotypes";
	private static final String CELL_LINE = "cell_line";
	private final CellLineSearch cellLineSearch;

	public EbiscVariantSearch(DataTypeInfo type, String baseUrl, String fileAccession, CellLineSearch cellLineSearch) {
		super(type, baseUrl, fileAccession);
		this.cellLineSearch = cellLineSearch;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> decorateVariant(Map<String, Object> v) {
		Object genotypes = v.get(GENOTYPES);
		if (genotypes != null) {
			for (Map<String, Object> genotype : (List<Map<String, Object>>) genotypes) {
				Optional<Map<String, Object>> metadata = cellLineSearch
						.getCellLine(String.valueOf(genotype.get(GENOTYPE_ID)));
				if (metadata.isPresent()) {
					genotype.put(CELL_LINE, metadata.get());
				}
			}
		}
		return v;
	}

}
