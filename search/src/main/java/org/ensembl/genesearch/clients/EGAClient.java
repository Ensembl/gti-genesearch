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

package org.ensembl.genesearch.clients;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.impl.HtsGetVariantSearch;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A simple standalone CLI client for use with {@link HtsGetVariantSearch}
 * 
 * @author dstaines
 *
 */
public class EGAClient {

    public static class Params extends ClientParams {

        @Parameter(names = "-url", description = "htsget url")
        private String url = null;

        @Parameter(names = "-ega_url", description = "EGA url")
        private String egaUrl = null;

        @Parameter(names = "-query", description = "JSON query string")
        private String query = "{}";

        @Parameter(names = "-fields", description = "Fields to retrieve")
        private List<String> resultFields = Collections.emptyList();

        @Parameter(names = "-limit", description = "Number of rows to retrieve")
        private int limit = 10;

        @Parameter(names = "-offset", description = "Place to start from")
        private int offset = 0;

        @Parameter(names = "-outfile", description = "File to write results to")
        private String outFile = null;

    }

    private final static Logger log = LoggerFactory.getLogger(EGAClient.class);

    public static void main(String[] args) throws InterruptedException, IOException {

        Params params = new Params();
        JCommander jc = new JCommander(params, args);
        jc.setProgramName(EGAClient.class.getSimpleName());

        Search search = new HtsGetVariantSearch(new DataTypeInfo(), params.url, params.egaUrl);

        List<Query> queries = new DefaultQueryHandler().parseQuery(params.query);
        QueryOutput fieldNames = QueryOutput.build(params.resultFields);

        log.info("Starting query");

        if (!StringUtils.isEmpty(params.outFile)) {

            SearchResult res = search.fetch(queries, fieldNames);
            log.info("Writing results to " + params.outFile);
            ObjectMapper mapper = new ObjectMapper();
            FileUtils.write(new File(params.outFile), mapper.writeValueAsString(res.getResults()),
                    Charset.defaultCharset());

        } else {

            QueryResult res = search.query(queries, fieldNames, Collections.emptyList(), 1, 10,
                    Collections.emptyList());
            log.info("Writing results to " + params.outFile);
            ObjectMapper mapper = new ObjectMapper();
            FileUtils.write(new File(params.outFile), mapper.writeValueAsString(res.getResults()),
                    Charset.defaultCharset());

        }

        log.info("Completed retrieval");

    }
}
