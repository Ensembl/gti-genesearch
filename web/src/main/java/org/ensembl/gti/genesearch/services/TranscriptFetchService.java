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

package org.ensembl.gti.genesearch.services;

import javax.ws.rs.Path;

import org.ensembl.genesearch.Search;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author dstaines
 *
 */
@Service
@Path("/transcripts/fetch")
public class TranscriptFetchService extends FetchService {

	/**
	 * @param provider
	 */
	@Autowired
	public TranscriptFetchService(EndpointSearchProvider provider) {
		super(provider);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.gti.genesearch.services.FetchService#getSearch()
	 */
	@Override
	public Search getSearch() {
		return provider.getTranscriptSearch();
	}

}
