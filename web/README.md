# Overview
Web and REST interfaces for gti-genesearch

# Installation and running
To build and run:
```
./gradlew clean :web:bootRepackage
java -Xmx2g -jar web/build/libs/web-0.1.jar --es.node=false --es.host=gti-es-0 --rest.url.eg=http://rest.ensemblgenomes.org/sequence/id --rest.url.ens=http://rest.ensembl.org/sequence/id
```

This will automatically connect to an elasticsearch cluster named genesearch on localhost:9300 as a transport client
To override, use the following command line options:
- to connect as a client node `--es.node=true`
- to override the cluster name `--es.cluster=mycluster`
- to override the ES host `--es.host=127.0.0.1`
- to override the ES port `--es.port=9300`

# Usage

The service is now available on http://localhost:8080/api with service descriptors from http://localhost:8080/api/application.wadl

## `/api/health`
This can be used to check if the server is running

## `/api/fieldinfo`
This can be used to check if the server is running

## `/api/genes/query` and `/api/genomes/query`
The `/api/genes/query` and `/api/genomes/query` endpoints allow the first n hits and a count plus facets to be returned as JSON. Both endpoints operate in the same way.

### GET
The `/api/genes/query` endpoint accepts the following arguments:
- `query` - a JSON-formatted string e.g. `"{"genome":"homo_sapiens","biotype":"protein_coding"}"`
- `fields` - a comma-separated list of fields e.g. `genome,name,description`. `*` can be used as a wildcard e.g. `*` to retrieve all fields
- `sort` - a comma-separated list of fields to sort by, which can be preceded by `+` or `-` to indicate ascending or descending e.g. `+name,-start`
- `limit` - number of hits to return e.g. 100
- `facets` - a comma-separated list of fields to facet over e.g. `facets=genome,biotype`
- `target` - optional name of target if genes are not required (e.g. `transcripts`, `translations`, `homologues`, `sequences`)
- `targetQuery` - optional additional queries passed to the secondary target

http://localhost:8080/api/genes/query?query={"genome":"nanoarchaeum_equitans_kin4_m"}&limit=5&fields=name,genome&sort=+name,-start&facets=biotype

This endpoint returns a JSON-formatted response containing the number of hits, the first `limit` hits and a list of facets e.g.
```
{
    "resultCount": 598,
    "results": [
        {
            "genome": "nanoarchaeum_equitans_kin4_m",
            "name": "5S_rRNA",
            "id": "EBG00001184723"
        },
        {
            "genome": "nanoarchaeum_equitans_kin4_m",
            "name": "5_8S_rRNA",
            "id": "EBG00001184731"
        }
    ],
    "facets": {
        "biotype": {
            "tRNA": 38,
            "protein_coding": 536,
            "rRNA": 23,
            "snRNA": 1
        }
    }
}
```

### POST
This is as above, but the parameters are supplied as pure JSON:
```
curl -H "Content-Type: application/json" -X POST --data '{"query":{"genome":"nanoarchaeum_equitans_kin4_m"},"limit":10, "fields":["name","genome"],"sort":["+name","-start"]}' http://localhost:8080/api/genes/query
```

## `/api/{object}/fetch` 
The `/api/genes/fetch` and `/api/genomes/fetch`  endpoints are intended for bulk retrieval and accepts the `query`, `fields`, `sort`, `target` and `targetQuery` parameters from `/query`. It can be invoked via GET and POST and returns the results as a JSON array, with no summary or facets available.

### GET
http://localhost:8080/api/genes/fetch?query={"genome":"nanoarchaeum_equitans_kin4_m"}&fields=name,genome&sort=+name,-start

### POST
```
curl -H "Content-Type: application/json" -X POST --data '{"query":{"genome":"nanoarchaeum_equitans_kin4_m"},"fields":["name","genome"],"sort":["+name","-start"]}' http://localhost:8080/api/genes/fetch
```

## `/api/{object}` and 
The `/api/genes` and `/api/genomes` endpoint are simple endpoints for retrieving complete documents for genes as JSON.

### GET (single gene or genome)
The gene stable ID or genome production name is supplied as a URL parameter:
http://localhost:8080/api/genes/NEQ392

### POST (multiple genes or genomes)
A JSON document containing a list of gene stable IDs or genome production names can be passed via POST:
```
curl -H "Content-Type: application/json" -X POST --data '["NEQ392","NEQ393"]' http://localhost:8080/api/genes
```

# Implementation notes
See [implementation.md] for more information on implementation and design.

# Copyright and Licensing
Copyright 1999-2016 EMBL-European Bioinformatics Institute

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.

You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.