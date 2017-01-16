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

package org.ensembl.gti.genesearch.services;

import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.query.DataTypeAwareQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for services dealing with SearchProviders
 * 
 * @author dstaines
 *
 */
public abstract class SearchBasedService {

	final Logger log = LoggerFactory.getLogger(this.getClass());
	private QueryHandler handler;
	protected final EndpointSearchProvider provider;

	/**
	 * @param provider provider for retrieving search
	 */
	public SearchBasedService(EndpointSearchProvider provider) {
		this.provider = provider;
	}

	private QueryHandler getHandler() {
		if (handler == null) {
			this.handler = new DataTypeAwareQueryHandler(getSearch().getDataType());
		}
		return handler;
	}

	/**
	 * @return instance of {@link Search} to use for query/fetch
	 */
	public abstract Search getSearch();

	protected List<Query> parseQuery(Map<String, Object> q) {
		return getHandler().parseQuery(q);
	}

}
