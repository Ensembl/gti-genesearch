package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.QueryUtils;

/**
 * {@link Search} using the htsget API provided internally by the EGA for
 * securely accessing VCF data. Uses {@link HtsGetClient} as abstraction over
 * htsget API, including authentication. The client requires the EGA REST
 * endpoint for determining which data a user has access to and htsget REST
 * endpoint for retrieving data.
 * 
 * This implementation supports querying based on a given dataset, a given file
 * or all data that the user has access to.
 * 
 * Directly supported query fields are:
 * <ul>
 * <li>seq_region_name</li>
 * <li>start</li>
 * <li>end</li>
 * <li>files - EGA file accessions</li>
 * <li>datasets - EGA dataset accessions</li>
 * </ul>
 * 
 * Authentication is delegated to the user, who must authenticate against the
 * htsget API to obtain an oauth token, passed as a query term "token".
 * 
 * In addition, if dataset level access is used, authentication against EGA's
 * separate REST interface is needed to obtain a session token, passed in as a
 * query term "session"
 * 
 * All other fields are applied as post-retrieval filters
 * 
 * 
 * @author dstaines
 *
 */
public class HtsGetVariantSearch implements Search {

    /**
     * Helper to process {@link Query} objects and return htsget arguments as a
     * POJO
     * 
     * @author dstaines
     *
     */
    protected final static class HtsGetArgs {

        public static final String TOKEN = "token";
        public static final String SESSION = "session";
        public static final String END = "end";
        public static final String START = "start";
        public static final String SEQ_REGION_NAME = "seq_region_name";
        public static final String FILES = "files";
        public static final String DATASETS = "datasets";

        public static HtsGetArgs build(List<Query> qs) {
            HtsGetArgs args = new HtsGetArgs();
            for (Query q : qs) {
                switch (q.getFieldName()) {
                case DATASETS:
                    args.datasets = q.getValues();
                    break;
                case FILES:
                    args.files = q.getValues();
                    break;
                case SEQ_REGION_NAME:
                    args.seqRegionName = q.getValues()[0];
                    break;
                case START:
                    args.start = Long.parseLong(q.getValues()[0]);
                    break;
                case END:
                    args.end = Long.parseLong(q.getValues()[0]);
                    break;
                case TOKEN:
                    args.token = q.getValues()[0];
                    break;
                case SESSION:
                    args.session = q.getValues()[0];
                    break;
                default:
                    args.queries.add(q);
                    break;
                }
            }
            return args;
        }

        String[] files;
        String[] datasets;
        String seqRegionName;
        long start;
        long end;
        String token;
        String session;

        List<Query> queries = new ArrayList<>();

        protected HtsGetArgs() {
        }
    }

    protected final HtsGetClient client;
    protected final DataTypeInfo dataType;

    /**
     * @param type
     *            metadata about search
     * @param baseUrl
     *            base URL for htsget API
     * @param egaBaseUrl
     *            base URL for EGA REST API
     */
    public HtsGetVariantSearch(DataTypeInfo type, String baseUrl, String egaBaseUrl) {
        this.client = new HtsGetClient(baseUrl, egaBaseUrl);
        this.dataType = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
     * java.util.List, org.ensembl.genesearch.QueryOutput)
     */
    @Override
    public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
        // extract URI arguments
        HtsGetArgs args = queryToArgs(queries);
        // consumer which will process data from the client
        Consumer<Map<String, Object>> fetchConsumer = v -> {
            if (QueryUtils.filterResultsByQueries.test(v, args.queries)) {
                consumer.accept(QueryUtils.filterFields(decorateVariant(v), fieldNames));
            }
        };
        // different queries mean different client methods
        if (args.files != null && args.files.length > 0) {
            client.getVariantsForFiles(args.files, args.seqRegionName, args.start, args.end, args.token, fetchConsumer);
        } else if (args.datasets != null && args.datasets.length > 0) {
            client.getVariantsForDatasets(args.datasets, args.seqRegionName, args.start, args.end, args.token,
                    args.session, consumer);
        } else {
            client.getVariants(args.seqRegionName, args.start, args.end, args.token, args.session, consumer);
        }
    }

    /**
     * Template method to extract and validate query terms that can be used with
     * htsget
     * 
     * @param queries
     * @return
     */
    protected HtsGetArgs queryToArgs(List<Query> queries) {
        HtsGetArgs args = HtsGetArgs.build(queries);
        return args;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#getDataType()
     */
    @Override
    public DataTypeInfo getDataType() {
        return dataType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#query(java.util.List,
     * org.ensembl.genesearch.QueryOutput, java.util.List, int, int,
     * java.util.List)
     */
    @Override
    public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
            List<String> sorts) {
        List<Map<String, Object>> results = new ArrayList<>();
        AtomicInteger n = new AtomicInteger(); // counter - atomic int allows
                                               // increment of effectively final
                                               // reference
        // extract URI arguments
        HtsGetArgs args = queryToArgs(queries);
        // create a consumer which will handle variant results
        Consumer<Map<String, Object>> consumer = v -> {
            if (QueryUtils.filterResultsByQueries.test(v, args.queries)) {
                int i = n.incrementAndGet();
                if (i > offset && i < offset + limit) {
                    results.add(QueryUtils.filterFields(decorateVariant(v), output));
                }
            }
        };

        if (args.files != null && args.files.length > 0) {
            // restrict to a particular
            client.getVariantsForFiles(args.files, args.seqRegionName, args.start, args.end, args.token, consumer);
        } else if (args.datasets != null && args.datasets.length > 0) {
            // if datasets have been specified
            client.getVariantsForDatasets(args.datasets, args.seqRegionName, args.start, args.end, args.token,
                    args.session, consumer);
        } else {
            //
            client.getVariants(args.seqRegionName, args.start, args.end, args.token, args.session, consumer);
        }
        return new QueryResult(n.get(), offset, limit, getFieldInfo(output), results, Collections.emptyMap());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
     */
    @Override
    public QueryResult select(String name, int offset, int limit) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ensembl.genesearch.Search#up()
     */
    @Override
    public boolean up() {
        return true;
    }

    /**
     * Add additional content to each variant document. This is a no-op stub.
     * 
     * @param v
     *            variant to decorate
     * @return decorated variant
     */
    protected Map<String, Object> decorateVariant(Map<String, Object> v) {
        // base method, do nothing
        return v;
    }

}
