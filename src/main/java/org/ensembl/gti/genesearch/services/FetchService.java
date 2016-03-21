package org.ensembl.gti.genesearch.services;

import static org.ensembl.gti.genesearch.services.FetchParams.stringToList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch;
import org.ensembl.genesearch.GeneSearch.QuerySort;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/fetch")
public class FetchService {

	final Logger log = LoggerFactory.getLogger(FetchService.class);
	protected final GeneSearch search;

	@Autowired
	public FetchService(GeneSearchProvider provider) {
		this.search = provider.getGeneSearch();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> fetch(@BeanParam FetchParams params) {
		log.info("query:" + params.toString());

		Collection<GeneQuery> queries = new DefaultQueryHandler()
				.parseQuery(params.getQueryString());

		List<QuerySort> sorts = stringToList(params.getSort())
				.stream()
				.map(sort -> new QuerySort(sort, QuerySort.SortDirection
						.valueOf(params.getSortDir().toUpperCase())))
				.collect(Collectors.toList());

		return search.fetch(queries, stringToList(params.getFields()), sorts);
	}

}
