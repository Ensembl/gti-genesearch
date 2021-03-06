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

package org.ensembl.genesearch.clients;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

/**
 * A simple standalone CLI client for an Elastic query service
 *
 * @author dstaines
 */
public class QueryClient {

    public static class Params extends ClientParams {

        @Parameter(names = "-query", description = "JSON query string")
        private String query = "{}";

        @Parameter(names = "-fields", description = "Fields to retrieve")
        private List<String> resultFields = Collections.emptyList();

        @Parameter(names = "-facets", description = "Fields to facet by")
        private List<String> facets = Collections.emptyList();

        @Parameter(names = "-sort", description = "Fields to sort by")
        private List<String> sorts = Collections.emptyList();

        @Parameter(names = "-limit", description = "Number of rows to retrieve")
        private int limit = 10;

        @Parameter(names = "-offset", description = "Place to start from")
        private int offset = 0;

        @Parameter(names = "-target", description = "Object target to flatten to")
        private String target = null;

        @Parameter(names = "-targetQuery", description = "Object target query to flatten to")
        private String targetQuery = null;

        @Parameter(names = "-outfile", description = "File to write results to")
        private String outFile = null;

    }

    private final static Logger log = LoggerFactory.getLogger(QueryClient.class);

    public static void main(String[] args) throws InterruptedException, IOException {

        Params params = new Params();
        JCommander jc = new JCommander(params, args);
        jc.setProgramName(QueryClient.class.getSimpleName());
        log.info("Creating client");
        Client client = ClientBuilder.buildClient(params);

        if (client == null) {
            jc.usage();
            System.exit(1);
        }

        Search search = new ESSearch(client, ESSearch.GENES_INDEX, ESSearch.GENE_ESTYPE,
                DataTypeInfo.fromResource("/gene_datatype_info.json"));

        List<Query> queries = new DefaultQueryHandler().parseQuery(params.query);

        log.info("Starting query");

        SearchResult res = search.query(queries, QueryOutput.build(params.resultFields), params.facets, params.offset,
                params.limit, params.sorts);

        if (!StringUtils.isEmpty(params.outFile)) {
            log.info("Writing results to " + params.outFile);
            FileUtils.write(new File(params.outFile), res.toString(), Charset.forName("UTF-8"));
        }

        log.info("Completed retrieval");

        log.info("Closing client");
        client.close();

    }
}
