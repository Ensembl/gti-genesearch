# REST interface

The REST interface is designed to support both the web interface and programmatic access and is implemented as Jersey REST services running within a Spring Boot application. 

The main `Application` uses a set of services (configured in `JerseyConfig`) which implement the GET and POST methods of the API. These can be grouped by base class:
* `QueryService` - query for objects by query, supporting faceting and sorting. Intended for use by a Javascript web interface. Implementations are:
    * `GeneQueryService`
    * `TranscriptQueryService`
    * `VariantQueryService`
    * `GenomeQueryService`
    * `ExpressionQueryService`
    * `CellLineQueryService`
* `FetchServiceQueryService` - retrieve full results sets by query. Intended for bulk download. Implementations are:
    * `GeneFetchService`
    * `TranscriptFetchService`
    * `VariantFetchService`
    * `GenomeFetchService`
    * `ExpressionFetchService`
    * `CellLineFetchService`
* `ObjectService` - retrieve complete objects by ID. Intended for bulk download or use by other web applications. Implementations are:
    * `GeneService`
    * `TranscriptService`
    * `VariantService`
    * `GenomeService`
    * `ExpressionService`
    * `CellLineService`
* `InfoService` - returns a JSON representation of available fields. Intended for use by a web interface. Implementations are:
    * `GeneInfoService`
    * `TranscriptInfoService`
    * `VariantInfoService`
    * `GenomeInfoService`
    * `ExpressionInfoService`
    * `CellLineInfoService`
* `HealthService` - returns the status of the service, examining each sub search in turn.
* `SwaggerService` - since swagger annotations don't seem to work with inheritance, this is a simple service to serve up swagger JSON files from the classpath that can then be used by the swagger UI.

Swagger documentation can be found under `/swagger/index.html`. This uses the following JSON swagger documents:
* [swagger.json](../../web/src/main/resources/swagger.json) - common definitions including shared models. Individual groups of services are defined:
    * [genes_swagger.json](../../web/src/main/resources/genes_swagger.json)
    * [transcripts_swagger.json](../../web/src/main/resources/transcripts_swagger.json)
    * [genomes_swagger.json](../../web/src/main/resources/genomes_swagger.json)
    * [variants_swagger.json](../../web/src/main/resources/variants_swagger.json)
    * [cell_lines_swagger.json](../../web/src/main/resources/cell_lines_swagger.json)
    * [expression_swagger.json](../../web/src/main/resources/expression_swagger.json)
    * [health_swagger.json](../../web/src/main/resources/health_swagger.json)

Examples of common usage can be found in [examples.md](examples.md).

Note that the `GET` and `POST` implementations carry out the same job, but pass parameters in different ways. They are passed as query params for GET and as a JSON request body for `POST`. REST purists would argue that `POST` is actually something that should be "`GET`-with-body" but this doesn't seem widely used, so `POST` is the pragmatic choice. Hey ho.

The abstract service implementations are responsible for processing data into JSON, XML and CSV using generic approaches. The concrete implementations mainly provide an appropriate `Search` instance for accessing genes, variations or genomes as appropriate - however, the ObjectService implementations need to construct the XML documents specifically for each type. An example showing how gene and genome services are implemented is shown below:
![Service Diagram](service_diagram.png)

The concrete `Search` implementations behind each service endpoint are provided by instances of the abstract class `EndpointSearchProvider`. These are used by service endpoint components to get an instance of `Search`. Different instances of `EndpointSearchProvider` provide different combinations of searches for different applications, and can be turned on and off by using Spring profiles which inject them into service components using Spring autowiring. For instance, `EnsemblVariationEndpointProvider` is used by default, so that (for instance) `VariantQueryService` would get an instance of `ESSearch` pointing to variant indices containing Ensembl data. However, if the profile is set to `ebisc` (e.g. by setting the property `spring.profiles.active=ebisc`), `VariantQueryService` is given an instance of `EbiscVariantSearch` which returns EBiSC specific data. Note that the EBiSC application is described in more detail in [EBiSC](ebisc.md).

# Copyright and Licensing
Copyright 1999-2016 EMBL-European Bioinformatics Institute

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.