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

import java.util.HashMap;
import java.util.Map;

import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;

/**
 * Class for providing a search and data type info for a supplied target type
 * 
 * @author dstaines
 *
 */
public class SearchRegistry {

	private final Map<SearchType, Search> searches = new HashMap<>();

	protected Map<SearchType, Search> getSearches() {
		return searches;
	}

	/**
	 * Register a given search object against a type
	 * 
	 * @param type
	 * @param search
	 * @return Provider (allows fluent-style calls)
	 */
	public SearchRegistry registerSearch(SearchType type, Search search) {
		getSearches().put(type, search);
		return this;
	}

	/**
	 * Find the search for a given type
	 * 
	 * @param type
	 * @return search instance for given type
	 */
	public Search getSearch(SearchType type) {
		return getSearches().get(type);
	}

}
