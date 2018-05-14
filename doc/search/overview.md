# Overview

The core of the system is the search API. This provides a simple abstraction for searching data from a variety of different database backends, from Elastic and Mongo to REST and htsget. This layer also allows basic joining between different sources.

# Implementation

The central point of entry for the search API is `org.ensembl.genesearch.Search`, an interface which must be implemented by each different backend technology implementation, or for specific functionality such as joining etc. See [Implementations](implementations.md) for more detail on individual implementations.

## Search
The `Search` interface provides two main sets of search methods - `fetch` and `query`. `Search` also includes default methods which are independent of implementation to reduce the numbers of methods required for an implementation.

`Search` methods use `Query` and `FieldInfo` objects to define the search terms used.  

### fetch

Fetch methods are intended to produce entire result sets matching a specified query, either by returning a `SearchResult` object containing all matched documents, or by writing Map objects to a supplied Consumer, which allows streaming of very large sets. There are a group of fetch methods for fetching by query, lists of IDs, single IDs etc.

### query

The query method returns a `QueryResult` object containing subset of results (based on offset and limit) parameters and supports faceting and sorting (where the underlying implementation supports it). It is intended for interactive use by a web interface so that the first few results are returned very quickly.

### getFieldInfo

`Search` also provides a `getFieldInfo` object to return type and further details for a list of names fields. This is used to populate `SearchResult` and `QueryResult` field lists, and is typically generated from JSON resources within the project e.g. `genes_datatype_info.json`

### select
This method is intended for basic auto-complete functionality e.g. show me all genomes starting 'Homo'. It is optional.

### up
This is a method to indicate whether the search is available, and is implementation-specific.

## Query
The `Query` object encapsulates parameters to be used when querying the `Search` interface. There are several types of `Query` possible, defined by an enum passed during construction.
* `TERM` - specifies a field and an exact match for 1 or more values
* `TEXT` - specifies a field and a sub-text match for 1 or more values
* `NUMBER` - accepts a numeric field and optional operators e.g. 1, 1.2, -1.2, >1.2, <1.3, 1.3-1.6 etc.
* `LOCATION` - accepts one of more specially formatted genomic location strings of the form `<name>:<start>:<end>[:<strand>]
* `NESTED` - a field and combines multiple sub-`Query` objects to be combined with an "AND" operator

Queries can be specified as JSON or generic Map structures, and transformed into `Query` objects by instances of `QueryHandler`. `DefaultQueryHandler` uses the contents of query fields to determine the query type whilst `DataTypeAwareQueryHandler` uses a `DataInfo` instance to look up the type of query from a predefined list.

Negation can be specified by prepending `!` to a query field e.g. `{"!genome":"homo_sapiens"}`

Note that different `Search` [implementations](implementations.md) support different sets of query types - please check support.

## QueryOutput
`QueryOutput` objects are passed to `fetch` and `query` to define which fields should be returned from a document. These objects can be nested to allow sub-documents to be returned. They are simple maps to allow flexibility.

## SearchResult
`SearchResult` encapsulates a set of results, plus a list of `FieldInfo` objects. This can be returned by `fetch`.

## QueryResult
`QueryResult` is an extension of `SearchResult` intended to encapsulates the first set of results returned by `query`, and adds a count and any facets.

## FieldInfo
`FieldInfo` objects are intended to inform a client about what a field result contains, and are added to `SearchResult` and `QueryResult`. Supported `FieldInfo` objects are also provided by `Search.getFieldInfo()` and provides hints on how to handle the field e.g. can it be sorted or faceted. These are unenforced in search and just intended as hints. 
