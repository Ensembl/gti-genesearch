# `/query`

## Example queries

Find all genes called BRCA2 in any genome:

`/genes/query?query={"name":"BRCA2"}`

To restrict to a given genome:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}`

To restrict to a results except in the given genome:

`/genes/query?query={"name":"BRCA2","!genome":"homo_sapiens"}`

To restrict to a given taxonomic lineage (40674 is Mammalia):

`/genes/query?query={"name":"BRCA2","lineage":"40674"}`

To find genes annotated with GO terms that are the children of a given term use the GO_expanded field:

`/genes/query?query={"GO_expanded":"GO:0016787"}`

To find specific GO term annotation (double-stranded break repair with mutant phenotype evidence):

`/genes/query?query={"GO":{"evidence":"IMP","term":"GO:0006302"}}`

To find genes with a particular PHI-base annotation (reduced virulence in wheat)

`/genes/query?query={"PHI":{"host":"4565","phenotype":"reduced virulence"}}`

To find genes with a particular UniProt cross-reference:

`/genes/query?query={"Uniprot/SWISSPROT":["P03886"]}`

To find genes matching any of a list of UniProt records:

`/genes/query?query={"Uniprot/SWISSPROT":["P03886","P03891","P00395","P00403","P03928"]}`

To find genes matching with different values of a number such as a start position:

`/genes/query?query={"genome":"homo_sapiens", "start":"45000-46000"}`

`/genes/query?query={"genome":"homo_sapiens", "start":">45000"}`

`/genes/query?query={"genome":"homo_sapiens", "start":"<=45000"}`

To find genes in a given location or locations:

`/genes/query?query={"genome":"homo_sapiens", "location":"1:45000-96000"}`

`/genes/query?query={"genome":"homo_sapiens", "location":"1:45000-96000:-1"}`

`/genes/query?query={"genome":"homo_sapiens", "location":["1:45000-52000","1:60000-96000"]}`

## Controlling output
Adding different fields:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}&fields=genome,name,description,start,end`

Returning results as a 2D array:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}&array=true&fields=genome,name,description,start,end`

Adding fields from a sub-object:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}&fields=genome,name,description,start,end,transcripts.name,transcripts.biotype`

Adding all fields:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}&fields=*`

## Joining other data types

Joining to homologues:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}&fields=["name","description",{"homologues":["division","name"]}]`

Joining to homologues and restricting further to just primates:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens","homologues":{"lineage":"9443"}}&fields=["name","description",{"homologues":["division","name"]}]`

Joining to variants:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}&fields=["name","description",{"variants":["id",{"locations":["seq_region_name","start","stop"]}]}]`

Joining to variants and showing only genes with variants:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens","variants":{"inner":1}}&fields="name","description",{"variants":["id",{"locations":["seq_region_name","start","stop"]}]}]`


Joining to variants and showing counts only:
`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}&fields=["name","description",{"variants":["count"]}]`

Joining to variants with given consequence:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens","variants":{"locations.annotations.consequences.name":"intron_variant"}}&fields=["name","description",{"variants":["id",{"locations":["seq_region_name","start","stop"]}]}]`

Joining to expression data:

`/genes/query?query={"name":"BRCA2","genome":"homo_sapiens"}&fields=["name","description",{"expression":["experiment_type","expression_level"]}]`

## Faceting
To facet by genome:

`/genes/query?query={"name":"BRCA2"}&facets=genome`

To facet by genome and biotype:

`/genes/query?query={"name":"BRCA2"}&facets=genome`

## Sorting
To sort by genome:

`/genes/query?query={"GO_expanded":"GO:0006302"}&fields=genome,name,start,end&sort=genome`

To facet by genome and start:

`/genes/query?query={"GO_expanded":"GO:0006302"}&fields=genome,name,start,end&sort=genome,start`

To facet by genome and start (descending):

`/genes/query?query={"GO_expanded":"GO:0006302"}&fields=genome,name,start,end&sort=genome,-start`

## Pagination
To show the first 10 entries:

`/genes/query?query={"name":"BRCA2","lineage":"40674"}&limit=10`

To show the next 10 entries:

`/genes/query?query={"name":"BRCA2","lineage":"40674"}&limit=10&offset=10`

NB: Performance is poor for very deep pagination - please use `/fetch` instead!

# `/fetch`

Retrieving all results as JSON:

`/genes/fetch?query={"name":"BRCA2"}`

Changing fields:

`/genes/fetch?query={"name":"BRCA2"}&fields=genome,name,description,start,end`

Returning results as a 2D array:

`/genes/fetch?query={"name":"BRCA2","genome":"homo_sapiens"}&array=true&fields=genome,name,description,start,end`

Retrieving all fields:

`/genes/query?query={"name":"BRCA2"}&fields=*`

Retrieving all results as XML (or pass the `Accept` properly as a request header):

`/genes/fetch?query={"name":"BRCA2"}&accept=application/xml`

Retrieving all results as CSV (or pass the `Accept` properly as a request header):

`/genes/fetch?query={"name":"BRCA2"}&accept=text/csv`

Retrieving genomic sequences for all results:

`/genes/fetch?query={"name":"BRCA2"}&fields=[{"sequences":[]}]`

Retrieving genomic sequences for all results with additional 100bp:

`/genes/fetch?query={"name":"BRCA2","sequences":{"expand3_prime":"100","expand5_prime":"100}}&fields=[{"sequences":[]}]`

Retrieving CDS sequences for all results:

`/genes/fetch?query={"name":"BRCA2","sequences":{"type":"cds"}}&fields=[{"sequences":[]}]`

Retrieving protein sequences for all results:

`/genes/fetch?query={"name":"BRCA2","sequences":{"type":"protein"}}&fields=[{"sequences":[]}]`
