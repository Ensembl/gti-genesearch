# REST interface

The REST interface is designed to support both the web interface and programmatic access and is implemented as Jersey REST services running within a Spring Boot application. 

The main `Application` uses a set of services (configured in `JerseyConfig`) which implement the GET and POST methods of the API. These can be grouped by base class:
* `QueryService` - query for objects by query, supporting faceting and sorting. Intended for use by a Javascript web interface. Implementations are:
** `GeneQueryService`
** `TranscriptQueryService`
** `VariantQueryService`
** `GenomeQueryService`
* `FetchServiceQueryService` - retrieve full results sets by query. Intended for bulk download. Implementations are:
** `GeneQueryService`
** `TranscriptQueryService`
** `VariantQueryService`
** `GenomeQueryService`
* `ObjectService` - retrieve complete objects by ID. Intended for bulk download or use by other web applications. Implementations are:
** `GeneQueryService`
** `TranscriptQueryService`
** `VariantQueryService`
** `GenomeQueryService`
* `InfoService` - returns a JSON representation of available fields. Intended for use by a web interface. Implementations are:
** `GeneQueryService`
** `TranscriptQueryService`
** `VariantQueryService`
** `GenomeQueryService`
* `HealthService` - returns the status of the service, examining each sub search in turn.

The main query services are shown below:

![Service Diagram](service_diagram.png)

The abstract service implementations are responsible for processing data into JSON, XML and CSV using generic approaches. The concrete implementations mainly provide an appropriate `Search` instance for accessing genes or genomes as appropriate (provided via `SearchProvider`) - however, the ObjectService implementations need to construct the XML documents specifically for each type.

# Copyright and Licensing
Copyright 1999-2016 EMBL-European Bioinformatics Institute

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.