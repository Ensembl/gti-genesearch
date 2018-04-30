# Overview

# Design


# Implementation

The current implementation uses Java, allowing the project to take full advantage of the extensive Java API elasticsearch, which allows the client layer to join the Elastic cluster without the need for unnecessary data transport. 

The interface to the search API is as follows:
* `org.ensembl.genesearch.Search`
* `org.ensembl.genesearch.Query`
* `org.ensembl.genesearch.QueryResult`
* `org.ensembl.genesearch.SearchResult`

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
The `Query` object encapsulates parameters to be used when querying the `Search` interface. There are several types of `Query` possible, defined by an enum passed during construction.
* `TERM` - specifies a field and an exact match for 1 or more values
* `TEXT` - specifies a field and a sub-text match for 1 or more values
* `NUMBER` - accepts a numeric field and optional operators e.g. 1, 1.2, -1.2, >1.2, <1.3, 1.3-1.6 etc.
* `LOCATION` - accepts one of more specially formatted genomic location strings of the form `<name>:<start>:<end>[:<strand>]
* `NESTED` - a field and combines multiple sub-`Query` objects to be combined with an "AND" operator

Queries can be specified as JSON or generic Map structures, and transformed into `Query` objects by instances of `QueryHandler`. `DefaultQueryHandler` uses the contents of query fields to determine the query type whilst `DataTypeAwareQueryHandler` uses a `DataInfo` instance to look up the type of query from a predefined list.

Negation can be specified by prepending `!` to a query field e.g. `{"!genome":"homo_sapiens"}`

## QueryResult
`QueryResult` encapsulates the first set of results, a count and any facets.