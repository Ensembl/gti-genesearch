# Overview

The main Search abstraction is just that, so concrete implementations are needed for each different technology. 

In addition, there are specialised implementations that can combine or rework data from concrete implementations.

All of this is described below.

# ElasticSearch implementation

From an Ensembl-centric standpoint, the most important implementation of `Search` is `org.ensembl.genesearch.impl.ESGeneSearch` which uses Elasticsearch (ES) via a Java API.

The main steps used in executing a query include:
1. Create an ES `QueryBuilder` instance from the supplied `Query` object
2. Create an ES search request object using the query, fields, limits, facets etc.
3. Execute the search
4. Process the ES Response (see `processResults`) and packaged into a `QueryResult` object with aggregations etc.

Note that sorts are passed as a list of field names, but prefixing the name with `+` or `-` sets the direction to ascending or descending as appropriate. Facets and fields are passed as lists of field names as well.

For fetch methods, the approach is different:
1. Analyse the query - for large numbers of query terms (e.g. lists of IDs), the query is split into pieces and executed separately with individual calls back to fetch. This is to avoid performance problems with very large results set
2. Build a search request as above (but no support for sort or faceting)
3. Set the search request to "scan/scroll", which is a more performant way of retrieving bulk data compared to offset/limit
4. Execute the search
5. Process all the hits using successive scan/scroll invocations (see `consumeAllHits`)

Some key methods to be aware of are:
* `processResults` - transform all hits in an ES response into Maps with `hitToMap`, flattening as required
* `hitToMap` - transform an individual hit document from ES into a Map
* `consumeAllHits` - process a search into a consumer using scan/scroll, invoking `consumeHits` on each method
* `consumeHits` - process all hits in a result set using `hitToMap` and pass to a consumer

Key classes of used by this implementation include:
* `org.ensembl.genesearch.impl.ESSearchBuilder` - code to transform a `Query` object into a `SearchBuilder`. This includes support for ranges, nested queries etc.
* `org.ensembl.genesearch.output.ResultRemodeller` - flatten a Map to the desired level using a specified path e.g. transcripts, transcripts.translations etc.

# REST-based implementations

## EVA retrieval

## Cell line retrieval

## Sequence retrieval
### `EnsemblRestSequenceSearch`
Support for sequence retrieval is provided by the Ensembl `/sequence/id` endpoint, access to which is provided via `org.ensembl.genesearch.impl.EnsemblRestSequenceSearch`. This implementation supports `fetch` only and expects a list of `Query` objects including one against `id` which are collated and POSTed against the REST endpoint in batches (default size 50). Any other `Query` objects are used to add extra URL parameters understood by the REST endpoint including `type`, `species`, `expand_5prime` and `expand_3prime`.  

### `DivisonAwareSequenceSearch` 
The REST sequence endpoints are currently limited in that EG and e! genomes are provided by two different URLs, and performance in EG is very poor unless a `genome` parameter is passed. To deal with these two limitations, `org.ensembl.genesearch.impl.DivisionAwareSequenceSearch` accepts nested queries where the top level field name is the name of the genome, with the nested sub-queries being those normally passed to `EnsemblRestSequenceSearch`. Each query is checked against a list of genomes from Ensembl (lazily loaded from an instance of `ESSearch` for genomes) and dispatched to one of two `EnsemblRestSequenceSearch` instances (EG or e!).

# MongoDB implementation
`MongoSearch` provides a baseline implementation for searches against Mongo databases, namely the EVA database of variants. This does not currently support facets or sorting, and all counts are returned as -1 since MongoDB does not return a count on a search and would require a second execution. This can be provided if needed.

*Important*: MongoDB's use of indices is not as comprehensive as that of Elastic. Particular combinations of filters may not have an index available. For instances, `annot.ct.ensg` alone will not use an index as the index requires the use of `annot.ct.ensg.so` first. This will need careful handling in any web interface.

# Solr implementation
`SolrSearch` provides a baseline implementation for searches against Solr servers, namely the GXA database of expression data. This does not currently support facets.

# HTSget implementation

# Specialised implementations

## Flattening implementation

The Elastic implementations implicitly support nesting, and the gene search contains transcripts as sub-objects. However, some times we need to 'flatten' results to sub-objects e.g. transcripts. This can be carried out using instances of `ESSearchFlatten` which flattens returned results to a specified target. For instance, if the target is "transcripts" then the documents are split to provide a list of documents representing each transcript. Any gene level fields are attached to each transcript. For instance, consider a query matching the following gene:
```
{
  id:"123", 
  name:"xyz", 
  description:"my gene", 
  transcripts:[
    {id:"123.1", name:"xyz1"}, 
    {id:"123.2", name:"xyz2"}
 ]
}
```
If the target is transcripts, then the following two objects would be returned:
```
[
  {gene.id:"123", id:"123.1", name:"xyz1"},
  {gene.id:"123", id:"123.2", name:"xyz2"}
]
```
Note that currently result counts and offsets etc. will still indicate the number of genes.

## Join-aware implementation

Although the majority of queries will be against the gene store, there are scenarios where the user wants objects from another search implementation to be returned. For example, one might want to search for kinase genes on chromosome 1 and then find variants associated with those genes. To support this, a join mechanism is implemented by `org.ensembl.genesearch.impl.JoinMergeSearch`. This is an abstract class which supports using the results from 1 query to build a second query as follows:
* decompose queries and fields to split them into "from" and "to" using `decomposeQueryFields`. These are packaged into `SubSearchParams` objects for "from" and "to"
* query the primary db including join field
* hash results (in batches)
* for the hits, query the secondary and then pull back and add to the results

For each "to" search, the fields used and strategies for joining are encapsulated in instances of `JoinStrategy`. `MergeStrategy` is an enum controlling whether results from the "to" search should simply be appended to each "from" document or merged into an existing field e.g. `homologues`.

Joining is triggered by specifying the type to join to in the fields list e.g. to retrieve IDs and chromosomes of linked variants:
`["id","name",{"variants":["_id","chr"]}]`

For large join sets, it may be best to get a count for the numbers of joined documents. This can be done by specifying the field `count` in the join field list e.g. `["id","name",{"variants":["count"]}]`

Lastly, joining can be used to restrict the "from" result set to only those that have results in the joined set. This is triggered by specifying the special query field `inner` in the join query e.g.
`{"biotype":"protein_coding","variants":{"inner":1}}`

Concrete implementations are:
- `org.ensembl.genesearch.impl.GeneSearch`
- `org.ensembl.genesearch.impl.TranscriptSearch`
- `org.ensembl.genesearch.impl.VariantSearch`
- `org.ensembl.genesearch.impl.ExpressionSearch`