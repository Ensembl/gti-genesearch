package org.ensembl.genesearch.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.info.DataTypeInfo;

public class HtsGetSingleFileVariantSearch extends HtsGetVariantSearch {

	private final String[] fileAccessions;
	
	public HtsGetSingleFileVariantSearch(DataTypeInfo type, String baseUrl, String fileAccession) {
		super(type, baseUrl, StringUtils.EMPTY);
		this.fileAccessions = new String[] {fileAccession};
	}

	@Override
	protected HtsGetArgs queryToArgs(List<Query> queries) {
		HtsGetArgs args = super.queryToArgs(queries);
		args.session = null;
		args.datasets = null;
		args.files = fileAccessions;
		return args;
	}
	
	

}
