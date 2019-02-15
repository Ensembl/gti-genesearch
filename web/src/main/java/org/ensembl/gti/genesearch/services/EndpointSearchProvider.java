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

package org.ensembl.gti.genesearch.services;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.elasticsearch.client.Client;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.clients.ClientBuilder;
import org.ensembl.genesearch.impl.DivisionAwareSequenceSearch;
import org.ensembl.genesearch.impl.ESSearch;
import org.ensembl.genesearch.impl.ESSearchFlatten;
import org.ensembl.genesearch.impl.ExpressionSearch;
import org.ensembl.genesearch.impl.GeneSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.impl.SolrSearch;
import org.ensembl.genesearch.impl.TranscriptSearch;
import org.ensembl.genesearch.impl.VariantSearch;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

/**
 * Utility class to allow construction and injection of searches for REST
 * endpoints. Base class sets up common services allowing addition of different
 * variation implementations
 * 
 * This uses an instance of {@link SearchRegistry} (which itself is used across
 * different searches where joins are needed. These two classes are different in
 * how they are used, though do similar things.
 * 
 * @author dstaines
 *
 */
public class EndpointSearchProvider {

    final Logger log = LoggerFactory.getLogger(this.getClass());
    protected Search variantSearch = null;
    protected Search geneSearch = null;
    protected Search genomeSearch = null;
    protected Search transcriptSearch = null;
    protected Search expressionSearch = null;
    protected Search expressionAnalyticsSearch = null;
    protected Search sequenceSearch = null;
    protected Client client = null;
    protected Search cellLineSearch = null;

    private SolrClient solrAnalyticsClient = null;
    private SolrClient solrExperimentsClient = null;
    private SearchRegistry registry = null;
    @Value("${es.host}")
    private String hostName;
    @Value("${es.cluster}")
    protected String clusterName;
    @Value("${es.port:9300}")
    protected int port;
    @Value("${es.node}")
    protected boolean node;
    @Value("${es.genes.index:genes}")
    protected String genesIndex = ESSearch.GENES_INDEX;
    @Value("${es.genomes.index:genomes}")
    protected String genomesIndex = ESSearch.GENOMES_INDEX;

    @Value("${rest.url.ens}")
    protected String ensRestUrl;
    @Value("${rest.url.eg}")
    protected String egRestUrl;

    @Value("${solr.expression.url:}")
    protected String solrAnalyticsUrl;
    @Value("${solr.experiments.url:}")
    protected String solrExperimentsUrl;

    public EndpointSearchProvider() {
    }

    public Client getESClient() {
        if (client == null) {
            if (node) {
                log.info("Joining ES cluster " + this.clusterName + " via " + this.hostName);
                client = ClientBuilder.buildClusterClient(this.clusterName, this.hostName);
            } else {
                log.info("Connecting to ES cluster " + this.clusterName + " on " + this.hostName + ":" + this.port);
                client = ClientBuilder.buildTransportClient(this.clusterName, this.hostName, this.port);
            }
        }
        return client;
    }

    public void setESClient(Client client) {
        this.client = client;
    }

    public SolrClient getSolrAnalyticsClient() {
        if (solrAnalyticsClient == null) {
            solrAnalyticsClient = new HttpSolrClient.Builder().withBaseSolrUrl(solrAnalyticsUrl).build();
        }
        return solrAnalyticsClient;
    }

    public void getSolrAnalyticsClient(SolrClient solrClient) {
        this.solrAnalyticsClient = solrClient;
    }

    public SolrClient getSolrExperimentsClient() {
        if (solrExperimentsClient == null) {
            solrExperimentsClient = new HttpSolrClient.Builder().withBaseSolrUrl(solrExperimentsUrl).build();
        }
        return solrExperimentsClient;
    }

    public void getSolrExperimentsClient(SolrClient solrClient) {
        this.solrExperimentsClient = solrClient;
    }

    public SearchRegistry getRegistry() {
        if (registry == null) {

            registry = new SearchRegistry();

            registerSearches(registry);

        }
        return registry;
    }

    protected void registerSearches(SearchRegistry reg) {

        registerESSearches(reg);
        registerEnsemblRestSearches(reg);
        registerExpressionSearches(reg);

    }

    protected void registerExpressionSearches(SearchRegistry reg) {
        DataTypeInfo expressionType = DataTypeInfo.fromResource("/datatypes/expression_datatype_info.json");
        DataTypeInfo expressionExperimentsType = DataTypeInfo
                .fromResource("/datatypes/expression_experiments_datatype_info.json");

        Search solrExpressionSearch = new SolrSearch(getSolrAnalyticsClient(), expressionType);
        Search solrExpressionExperimentsSearch = new SolrSearch(getSolrExperimentsClient(), expressionExperimentsType);

        reg.registerSearch(SearchType.EXPRESSION_ANALYTICS, solrExpressionSearch);
        reg.registerSearch(SearchType.EXPRESSION_EXPERIMENTS, solrExpressionExperimentsSearch);

        expressionSearch = new ExpressionSearch(reg);

        reg.registerSearch(SearchType.EXPRESSION, expressionSearch);
    }

    protected void registerEnsemblRestSearches(SearchRegistry reg) {
        // Ensembl REST searches
        DataTypeInfo seqType = DataTypeInfo.fromResource("/datatypes/sequences_datatype_info.json");
        Search seqSearch = new DivisionAwareSequenceSearch(registry.getSearch(SearchType.GENOMES), seqType,
                getEnsRestUrl(), getEgRestUrl());
        reg.registerSearch(SearchType.SEQUENCES, seqSearch);
    }

    protected void registerESSearches(SearchRegistry reg) {
        // Elastic based searches
        DataTypeInfo geneType = DataTypeInfo.fromResource("/datatypes/genes_datatype_info.json");
        DataTypeInfo genomeType = DataTypeInfo.fromResource("/datatypes/genomes_datatype_info.json");
        DataTypeInfo transcriptType = DataTypeInfo.fromResource("/datatypes/transcripts_datatype_info.json");

        Search esGenomeSearch = new ESSearch(getESClient(), genomesIndex, ESSearch.GENOME_ESTYPE, genomeType);
        Search esGeneSearch = new ESSearch(getESClient(), genesIndex, ESSearch.GENE_ESTYPE, geneType);
        Search esTranscriptSearch = new ESSearchFlatten(getESClient(), genesIndex, ESSearch.GENE_ESTYPE, "transcripts",
                "genes", transcriptType);

        reg.registerSearch(SearchType.GENES, esGeneSearch).registerSearch(SearchType.TRANSCRIPTS, esTranscriptSearch)
                .registerSearch(SearchType.HOMOLOGUES, esGeneSearch).registerSearch(SearchType.GENOMES, esGenomeSearch);
    }

    public Search getGeneSearch() {
        if (geneSearch == null) {
            geneSearch = new GeneSearch(getRegistry());
            assertHasSearch(geneSearch, "gene");
        }
        return geneSearch;
    }

    public Search getGenomeSearch() {
        if (genomeSearch == null) {
            genomeSearch = getRegistry().getSearch(SearchType.GENOMES);
            assertHasSearch(genomeSearch, "genome");
        }
        return genomeSearch;
    }

    public void setGeneSearch(Search search) {
        this.geneSearch = search;
    }

    public void setGenomeSearch(Search search) {
        this.genomeSearch = search;
    }

    public String getEnsRestUrl() {
        return ensRestUrl;
    }

    public void setEnsRestUrl(String ensRestUrl) {
        this.ensRestUrl = ensRestUrl;
    }

    public String getEgRestUrl() {
        return egRestUrl;
    }

    public void setEgRestUrl(String egRestUrl) {
        this.egRestUrl = egRestUrl;
    }

    public Search getTranscriptSearch() {
        if (transcriptSearch == null) {
            transcriptSearch = new TranscriptSearch(getRegistry());
            assertHasSearch(transcriptSearch, "transcript");
        }
        return transcriptSearch;
    }

    public void setTranscriptSearch(Search transcriptSearch) {
        this.transcriptSearch = transcriptSearch;
    }

    public Search getVariantSearch() {
        if (variantSearch == null) {
            variantSearch = new VariantSearch(getRegistry());
            assertHasSearch(variantSearch, "variant");
        }
        return variantSearch;
    }

    public void setVariantSearch(Search variantSearch) {
        this.variantSearch = variantSearch;
    }

    public Search getExpressionSearch() {
        if (expressionSearch == null) {
            expressionSearch = new ExpressionSearch(getRegistry());
            assertHasSearch(expressionSearch, "expression");
        }
        return expressionSearch;
    }

    public Search getSequenceSearch() {
        if (sequenceSearch == null) {
            sequenceSearch = getRegistry().getSearch(SearchType.SEQUENCES);
            assertHasSearch(sequenceSearch, "sequence");
        }
        return sequenceSearch;
    }

    public Search getCellLineSearch() {
        if (this.cellLineSearch == null) {
            this.cellLineSearch = getRegistry().getSearch(SearchType.CELL_LINES);
            assertHasSearch(cellLineSearch, "cell_line");
        }
        return this.cellLineSearch;
    }

    public void setCellLineSearch(Search cellLineSearch) {
        this.cellLineSearch = cellLineSearch;
    }

    /**
     * Helper method to assert that we have a particular search and throw an
     * exception if we don't
     * 
     * @param search
     * @param name
     *            name to use in message
     */
    protected static void assertHasSearch(Search search, String name) {
        if (search == null) {
            throw new UnsupportedOperationException("Search type " + name + " is not implemented in this interface");
        }
    }
}
