package org.ensembl.genesearch.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.utils.QueryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CellLineSearch implements Search {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	private final String url;
	private final String user;
	private final String apiKey;
	private final ObjectMapper mapper = new ObjectMapper();
	private final DataTypeInfo info;

	public CellLineSearch(DataTypeInfo info, String url, String user, String apiKey) {
		this.url = url;
		this.user = user;
		this.apiKey = apiKey;
		this.info = info;
	}

	private List<Map<String, Object>> cellLines;

	@SuppressWarnings("unchecked")
	protected List<Map<String, Object>> getCellLines() {
		if (cellLines == null) {
			// retrieve all cell lines
			String uri = url + "?username=" + user + "&api_key=" + apiKey;
			log.info("Querying base " + uri);

			int offset = 0;
			int resultCnt = 0;
			int limit = 100;
			cellLines = new ArrayList<>();
			do {
				try {
					ResponseEntity<String> response = new RestTemplate()
							.getForEntity(uri + "&offset=" + offset + "&limit=" + limit, String.class);
					if (response.getStatusCode() != HttpStatus.OK) {
						throw new RestSearchException(uri, response.getBody(), response.getStatusCode());
					}
					JsonNode body = mapper.readTree(response.getBody());
					log.info("Response retrieved");
					RestBasedSearch.resultsToStream(body.get("objects")).map(n -> mapper.convertValue(n, Map.class))
							.map(n -> {
								n.remove("batches");
								n.remove("status_log");
								return n;
							}).forEach(n -> cellLines.add(n));
					if (resultCnt == 0) {
						resultCnt = Integer.parseUnsignedInt(body.get("meta").get("total_count").asText());
					}
				} catch (IOException e) {
					throw new RestSearchException("Could not parse response body", url, e);
				}
				log.info("Executing fetch");
				log.info("Fetch executed");
				offset += limit;
			} while (resultCnt > 0 && offset < resultCnt);
			log.info(cellLines.size() + " cell lines retrieved");
		}
		return cellLines;
	}

	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput fieldNames) {
		getCellLines().stream().map(v -> (Map<String, Object>) mapper.convertValue(v, Map.class))
				.filter(v -> QueryUtils.filterResultsByQueries.test(v, queries))
				.map(v -> QueryUtils.filterFields(v, fieldNames)).forEach(consumer);
	}

	@Override
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {
		AtomicLong n = new AtomicLong(0);
		List<Map<String, Object>> results = getCellLines().stream()
				.map(v -> (Map<String, Object>) mapper.convertValue(v, Map.class))
				.filter(v -> QueryUtils.filterResultsByQueries.test(v, queries))
				.map(v -> QueryUtils.filterFields(v, output)).map(node -> {
					n.incrementAndGet();
					return node;
				}).skip(offset).limit(limit).collect(Collectors.toList());
		return new QueryResult(n.longValue(), (long) offset, (long) limit, getFieldInfo(output), results,
				Collections.emptyMap());
	}

	@Override
	public QueryResult select(String name, int offset, int limit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DataTypeInfo getDataType() {
		return info;
	}

	@Override
	public boolean up() {
		return true;
	}
}
