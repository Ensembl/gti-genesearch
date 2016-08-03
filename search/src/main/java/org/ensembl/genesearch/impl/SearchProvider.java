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

package org.ensembl.genesearch.impl;

import java.util.HashMap;
import java.util.Map;

import org.ensembl.genesearch.Search;

/**
 * Class for providing a search for a supplied target type
 * @author dstaines
 *
 */
public class SearchProvider {

	private final Map<SearchType, Search> searches = new HashMap<>();

	/**
	 * Register a given search object against a type
	 * @param type
	 * @param search
	 * @return Provider (allows fluent-style calls)
	 */
	public SearchProvider registerSearch(SearchType type, Search search) {
		searches.put(type,search);
		return this;
	}
	
	/**
	 * Find the search for a given type
	 * @param type
	 * @return
	 */
	public Search getSearch(SearchType type) {
		return searches.get(type);
	}

}
