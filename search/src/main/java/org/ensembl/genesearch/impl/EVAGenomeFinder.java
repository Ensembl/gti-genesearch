package org.ensembl.genesearch.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchResult;
import org.ensembl.genesearch.info.FieldType;
import org.ensembl.genesearch.utils.DataUtils;

/**
 * Utility class used by {@link EVAVariantRestSearch} to translate a standard
 * Ensembl genome name into the code used internally by EVA. Requires
 * {@link Search} for Ensembl genomes (e.g. {@link ESSearch}) and {@link Search}
 * for EVA genomes (e.g. {@link EVAGenomeRestSearch})
 * 
 * @author dstaines
 *
 */
public class EVAGenomeFinder {

    private final Search evaGenomeSearch;
    private final Search ensemblGenomeSearch;
    private final Map<String, String> genomeNames = new HashMap<>();

    /**
     * @param evaGenomeSearch
     *            search interface on EVA genomes
     * @param ensemblGenomeSearch
     *            search interface on EG genomes
     */
    public EVAGenomeFinder(Search evaGenomeSearch, Search ensemblGenomeSearch) {
        this.evaGenomeSearch = evaGenomeSearch;
        this.ensemblGenomeSearch = ensemblGenomeSearch;
    }

    /**
     * Look up the name used by EVA for a given Ensembl Genome
     * 
     * @param genomeName
     * @return
     */
    public String getEVAGenomeName(String genomeName) {
        String evaName = genomeNames.get(genomeName);
        if (evaName == null) {
            Map<String, Object> eGenome = ensemblGenomeSearch.fetchById(genomeName);
            if (eGenome == null) {
                throw new IllegalArgumentException("Could not find details for genome " + genomeName);
            }
            // use the INSDC accession as the only universal linker
            String accession = DataUtils.getObjValsForKey(eGenome, "assembly.accession").iterator().next();
            SearchResult evaGenomeRes = evaGenomeSearch.fetch(
                    Arrays.asList(new Query(FieldType.TERM, "assemblyAccession", accession)),
                    new QueryOutput("taxonomyCode", "assemblyCode"));
            if (!evaGenomeRes.getResults().isEmpty()) {
                Map<String, Object> evaGenome = evaGenomeRes.getResults().get(0);
                evaName = evaGenome.get("taxonomyCode") + "_" + evaGenome.get("assemblyCode");
            }
            genomeNames.put(genomeName, evaName);
        }
        return evaName;
    }

}
