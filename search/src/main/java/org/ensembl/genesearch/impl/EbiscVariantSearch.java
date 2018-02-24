package org.ensembl.genesearch.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ensembl.genesearch.info.DataTypeInfo;

/**
 * hts-get based search that uses a secondary search for decoration of results
 * with cell line metadata
 * 
 * @author dstaines
 * 
 */
public class EbiscVariantSearch extends HtsGetSingleFileVariantSearch {

	private static final String GENOTYPE_ID = "id";
	private static final String GENOTYPES = "genotypes";
	private static final String CELL_LINE = "cell_line";
	private final CellLineSearch cellLineSearch;

	public EbiscVariantSearch(DataTypeInfo type, String baseUrl, String fileAccession, CellLineSearch cellLineSearch) {
		super(type, baseUrl, fileAccession);
		this.cellLineSearch = cellLineSearch;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> decorateVariant(Map<String, Object> v) {
		Object genotypes = v.get(GENOTYPES);
		if (genotypes != null) {
			for (Map<String, Object> genotype : (List<Map<String, Object>>) genotypes) {
				Optional<Map<String, Object>> metadata = cellLineSearch
						.getCellLine(String.valueOf(genotype.get(GENOTYPE_ID)));
				if (metadata.isPresent()) {
					genotype.put(CELL_LINE, metadata.get());
				}
			}
		}
		return v;
	}

}
