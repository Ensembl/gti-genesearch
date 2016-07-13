# Overview
Elasticsearch provides an exceptionally rich, flexble and powerful set of REST interfaces for searching and retrieving data. However, this project provides an additional layer on top of this for the following reasons:
- any end user should be insulated from changes in implementation
- any end user should be insulated from the complexity exposed by Elasticsearch
- optimal queries can be generated using specific business logic
- the elasticsearch scan/scroll interface can be wrapped in a stream to allow efficient transfer of large result sets 

# Installation

This part of the gti-genesearch package is intended to be used as an API by other applications using gradle for dependency management, but an application distribution can be built that contains some example command line applications:

To build the application:
```

```

# Implementation

The current implementation uses Java, allowing the project to take full advantage of the extensive Java API elasticsearch, which allows the client layer to join the Elastic cluster without the need for unnecessary data transport. 

The interface to the search API is as follows:
* `org.ensembl.genesearch.Search`
* `org.ensembl.genesearch.Query`
* `org.ensembl.genesearch.QueryResult`

## Search
The `Search` interface provides two main sets of search methods - fetch and query.

Fetch methods are intended to produce entire result sets matching a specified query, either by returning a List of nested Map objects representing matched documents, or by writing Map objects to a supplied Consumer, which allows streaming of very large sets. There are a group of fetch methods for fetching by query, lists of IDs, single IDs etc.

The query method returns a `QueryResult` object subset of results (based on offset and limit) parameters and supports faceting and sorting.

Both sets of methods can consume `Query` objects, support sorting and allow a subset of fields to be returned. Fields can be leaves or entire sections of a hierarchy e.g. "name" (the gene name), "transcripts" (all transcripts), "transcripts.name" (names of all transcripts).

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

## QueryResult
`QueryResult` encapsulates the first set of results, a count and any facets.

# ElasticSearch implementation


# Copyright and Licensing
Copyright 1999-2016 EMBL-European Bioinformatics Institute

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.