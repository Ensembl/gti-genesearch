package org.ensembl.genesearch.query;

import java.util.List;
import java.util.Map;

import org.ensembl.genesearch.GeneQuery;

public interface QueryHandler {
	
	public List<GeneQuery> parseQuery(String json);
	public List<GeneQuery> parseQuery(Map<String,Object> query);

}
