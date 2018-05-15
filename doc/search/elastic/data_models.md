# Implementation

The data model used as two main data types, `gene` and `genome` where `gene` is a child of `genome`. Each is a simple nested document - for more details, please see the [`gene`](src/main/resources/gene_mapping.json) and [`genome`](src/main/resources/genome_mapping.json) mapping files used by Elasticsearch.

However, roughly speaking, a genome document follows the following general structure:
* dbname
* division etc.
* organism
    * name
    * lineage
    * taxonomy ID
    * aliases
* assembly
    * name
    * accession
    * level

and a gene document the following:
* id
* name
* coordinates
* xrefs
* transcripts
    * id 
    * name
    * coordinates
    * xrefs 
    * translations
        * id
        * name
        * xrefs
        * protein features
             * coordinates
             * name etc. 

Gene documents are produced by the Ensembl [bulk fetcher](https://github.com/Ensembl/ensembl-production/blob/master/modules/Bio/EnsEMBL/Production/DBSQL/BulkFetcher.pm). Gene documents are then restructured by [JsonRemodeller](https://github.com/Ensembl/ensembl-production/blob/master/modules/Bio/EnsEMBL/Production/Pipeline/JSON/JsonRemodeller.pm). This carries out the following steps:
- expansion of taxonomy IDs to provide a lineage
- collection of xrefs from sub-objects onto parents eg. copy all xrefs from each transcript to the parent gene
- collation of xrefs into individual fields e.g. PDB, Uniprot/SWISSPROT etc.
- expansion of ontology terms to parents e.g. GO and GO_expanded

Note that this process (and also the creation of genome and variant documents) will in future be carried out by the new [unified search dump pipeline](https://github.com/Ensembl/ensembl-production/tree/feature/unified_search_dumps).