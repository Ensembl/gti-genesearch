package org.ensembl.gti.genesearch.services;

import static org.ensembl.gti.genesearch.services.FetchParams.stringToList;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.ensembl.genesearch.GeneQuery;
import org.ensembl.genesearch.GeneSearch;
import org.ensembl.genesearch.GeneSearch.QuerySort;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/query")
public class QueryService {

	final Logger log = LoggerFactory.getLogger(QueryService.class);
	protected final GeneSearch search;
	@Autowired
	public QueryService(GeneSearchProvider provider) {
		this.search = provider.getGeneSearch();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public QueryResult query(@BeanParam QueryParams params) {

		log.info("query:|" + params.getQueryString() + "|");
		log.info("fields:|" + params.getFields() + "|");
		log.info("facets:|" + params.getFacets() + "|");
		log.info("sorts:|" + params.getSort() + "|");

		Collection<GeneQuery> queries = new DefaultQueryHandler()
				.parseQuery(params.getQueryString());

		List<QuerySort> sorts = stringToList(params.getSort())
				.stream()
				.map(sort -> new QuerySort(sort, QuerySort.SortDirection
						.valueOf(params.getSortDir().toUpperCase())))
				.collect(Collectors.toList());

		return search.query(queries, stringToList(params.getFields()),
				stringToList(params.getFacets()), params.getLimit(), sorts);
	}

}
