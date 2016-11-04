# Overview
Elasticsearch provides an exceptionally rich, flexble and powerful set of REST interfaces for searching and retrieving data. However, this project provides an additional layer on top of this for the following reasons:
- any end user should be insulated from changes in implementation
- any end user should be insulated from the complexity exposed by Elasticsearch
- optimal queries can be generated using specific business logic
- the elasticsearch scan/scroll interface can be wrapped in a stream to allow efficient transfer of large result sets 

# Installation

This part of the gti-genesearch package is intended to be used as an API by other applications using gradle for dependency management, but an application distribution can be built that contains an example command line application:

To build the example application:
```
./gradlew clean :search:distTar
tar xvf search/build/distributions/search-0.2.tar
cd search-0.2
./bin/search -help # show the options
```

# Implementation

The current implementation uses Java, allowing the project to take full advantage of the extensive Java API elasticsearch, which allows the client layer to join the Elastic cluster without the need for unnecessary data transport. 

The interface to the search API is as follows:
* `org.ensembl.genesearch.Search`
* `org.ensembl.genesearch.Query`
* `org.ensembl.genesearch.QueryResult`

## Search
The `Search` interface provides two main sets of search methods - fetch and query. `Search` also includes default methods which are independent of implementation to reduce the numbers of methods required for an implementation.

Fetch methods are intended to produce entire result sets matching a specified query, either by returning a `SearchResult` object containing all matched documents, or by writing Map objects to a supplied Consumer, which allows streaming of very large sets. There are a group of fetch methods for fetching by query, lists of IDs, single IDs etc. Query documents may either be genes or genomes, something which can be specified when constructing an instance of `Search`.

The query method returns a `QueryResult` object containing subset of results (based on offset and limit) parameters and supports faceting and sorting.

Both sets of methods can consume `Query` objects, support sorting and allow a subset of fields to be returned. Fields can be leaves or entire sections of a hierarchy e.g. "name" (the gene name), "transcripts" (all transcripts), "transcripts.name" (names of all transcripts).

`Search` also provides a `getFieldInfo` object to return type and further details for a list of names fields. This is used to populate `SearchResult` and `QueryResult` field lists.

Finally, both groups of methods support optional targets to control the type of object returned. By default, the target is the gene object, but a sub-object can be specified. For instance, if the target is "transcripts" then the documents are split to provide a list of documents representing each transcript. Any gene level fields are attached to each transcript. For instance, consider a query matching the following gene:
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

## Query
The `Query` object encapsulates parameters to be used when querying the `Search` interface. There are four types of `Query` possible, defined by an enum passed during construction.
* `TERM` - specifies a field and an exact match for 1 or more values
* `TEXT` - specifies a field and a sub-text match for 1 or more values
* `RANGE` - accepts a field plus start and end e.g. for a genomic location
* `NESTED` - a field and combines multiple sub-`Query` objects to be combined with an "AND" operator
_Note:_ This may be changed to a hierarchy of classes in due course

Queries can be specified as JSON or generic Map structures, and transformed into `Query` objects by instances of `QueryHandler` of which there is currently only one instance, `DefaultQueryBuilder`.

## QueryResult
`QueryResult` encapsulates the first set of results, a count and any facets.

# ElasticSearch implementation

The main implementation of `Search` is `org.ensembl.genesearch.impl.ESGeneSearch` which uses Elasticsearch (ES) via a Java API.

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

# Sequence retrieval
## `EnsemblRestSequenceSearch`
Support for sequence retrieval is provided by the Ensembl `/sequence/id` endpoint, access to which is provided via `org.ensembl.genesearch.impl.EnsemblRestSequenceSearch`. This implementation supports `fetch` only and expects a list of `Query` objects including one against `id` which are collated and POSTed against the REST endpoint in batches (default size 50). Any other `Query` objects are used to add extra URL parameters understood by the REST endpoint including `type`, `species`, `expand_5prime` and `expand_3prime`.  

## `DivisonAwareSequenceSearch` 
The REST sequence endpoints are currently limited in that EG and e! genomes are provided by two different URLs, and performance in EG is very poor unless a `genome` parameter is passed. To deal with these two limitations, `org.ensembl.genesearch.impl.DivisionAwareSequenceSearch` accepts nested queries where the top level field name is the name of the genome, with the nested sub-queries being those normally passed to `EnsemblRestSequenceSearch`. Each query is checked against a list of genomes from Ensembl (lazily loaded from an instance of `ESSearch` for genomes) and dispatched to one of two `EnsemblRestSequenceSearch` instances (EG or e!).

# Join-aware implementation

Although the majority of queries will be against the gene store, there are scenarios where the user wants objects from another search implementation to be returned. For example, one might want to search for kinase genes on chromosome 1 and then find variants associated with those genes. To support this, a join mechanism is implemented by `org.ensembl.genesearch.impl.JoinMergeSearch`. This is an abstract class which supports using the results from 1 query to build a second query as follows:
* decompose queries and fields to split them into "from" and "to" using `decomposeQueryFields`. These are packaged into `SubSearchParams` objects for "from" and "to"
* query the primary db including join field
* hash results (in batches)
* for the hits, query the secondary and then pull back and add to the results

For each "to" search, the fields used and strategies for joining are encapsulated in instances of `JoinStrategy`. `MergeStrategy` is an enum controlling whether results from the "to" search should simply be appended to each "from" document or merged into an existing field e.g. `homologues`.

A concrete implementation is `org.ensembl.genesearch.impl.GeneSearch` which supports homologue, transcript and sequence querying using `ESSearch` and `DivisionAwareSequenceSearch`. 

Subsequent development will implement this approach for variants.

# Copyright and Licensing
Copyright 1999-2016 EMBL-European Bioinformatics Institute

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.