package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.QueryUtils;

public class HtsGetVariantSearch implements Search {

	protected final static class HtsGetArgs {

		public static final String TOKEN = "token";
		public static final String SESSION = "session";
		public static final String END = "end";
		public static final String START = "start";
		public static final String SEQ_REGION_NAME = "seq_region_name";
		public static final String FILES = "files";
		public static final String DATASETS = "datasets";
		public static final String LOCATION = "location";

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
				case LOCATION:
					args.setLocation(q.getValues()[0]);
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

		Pattern p = Pattern.compile("([^:]+):([0-9]+)(-([0-9]+))?");
		protected void setLocation(String location) {
			Matcher m = p.matcher(location);
			if(m.matches()) {
				seqRegionName = m.group(1);
				start = Integer.parseInt(m.group(2));
				if(m.groupCount()==4) {
					end = Integer.parseInt(m.group(4));
				}
			} else {
				throw new IllegalArgumentException("Could not parse location string "+location);
			}
	
		}
		
		protected HtsGetArgs() {
		}
	}

	protected final HtsGetClient client;
	protected final DataTypeInfo dataType;

	public HtsGetVariantSearch(DataTypeInfo type, String baseUrl, String egaBaseUrl) {
		this.client = new HtsGetClient(baseUrl, egaBaseUrl);
		this.dataType = type;
	}

	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
		// extract URI arguments
		HtsGetArgs args = queryToArgs(queries);
		Consumer<Map<String, Object>> fetchConsumer = v -> {
			Optional<Map<String, Object>> v2 = queryAndFilter(args, v);
			if (v2.isPresent()) {
				consumer.accept(QueryUtils.filterFields(decorateVariant(v2.get()), fieldNames));
			}
		};
		if (args.files != null && args.files.length > 0) {
			client.getVariantsForFiles(args.files, args.seqRegionName, args.start, args.end, args.token, fetchConsumer);
		} else if (args.datasets != null && args.datasets.length > 0) {
			client.getVariantsForDatasets(args.datasets, args.seqRegionName, args.start, args.end, args.token,
					args.session, consumer);
		} else {
			client.getVariants(args.seqRegionName, args.start, args.end, args.token, args.session, consumer);
		}
	}

	protected Optional<Map<String, Object>> queryAndFilter(HtsGetArgs args, Map<String, Object> v) {
		Optional<Map<String,Object>> v2 = Optional.empty();
		for(Query q: args.queries) {
			v2 = QueryUtils.queryAndFilter(v, q);
			if(v2.isPresent()) {
				v = v2.get();
			} else {
				break;
			}
		}
		return v2;
	}

	protected HtsGetArgs queryToArgs(List<Query> queries) {
		HtsGetArgs args = HtsGetArgs.build(queries);
		if(StringUtils.isEmpty(args.token)) {
			throw new IllegalArgumentException("Access token not set");
		}
		return args;
	}

	@Override
	public DataTypeInfo getDataType() {
		return dataType;
	}

	@Override
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {
		List<Map<String, Object>> results = new ArrayList<>();
		AtomicInteger n = new AtomicInteger();
		// extract URI arguments
		HtsGetArgs args = queryToArgs(queries);
		Consumer<Map<String, Object>> consumer = v -> {
			Optional<Map<String, Object>> v2 = queryAndFilter(args, v);
			if(v2.isPresent()) {
				int i = n.incrementAndGet();
				if (i > offset && i < offset + limit) {
					results.add(QueryUtils.filterFields(decorateVariant(v), output));
				}
			}
		};
		if (args.files != null && args.files.length > 0) {
			client.getVariantsForFiles(args.files, args.seqRegionName, args.start, args.end, args.token, consumer);
		} else if (args.datasets != null && args.datasets.length > 0) {
			client.getVariantsForDatasets(args.datasets, args.seqRegionName, args.start, args.end, args.token,
					args.session, consumer);
		} else {
			client.getVariants(args.seqRegionName, args.start, args.end, args.token, args.session, consumer);
		}
		return new QueryResult(n.get(), offset, limit, getFieldInfo(output), results, Collections.emptyMap());
	}

	@Override
	public QueryResult select(String name, int offset, int limit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean up() {
		return true;
	}
	
	protected Map<String,Object> decorateVariant(Map<String,Object> v) {
		// base method, do nothing
		return v;
	}

}
