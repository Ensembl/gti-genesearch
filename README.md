h1. About
Web and REST interfaces for gti-genesearch

h1. Installation and running
To use:
# build a jar
./gradlew compile
# run the jar
java -jar build/libs/gti-genesearch-web-0.1.jar

This will automatically connect to an elasticsearch cluster named genesearch on localhost:9300 as a transport client
To override, use the following command line options:
# to connect as a client node `-es.node=true`
# to override the cluster name `-es.cluster=mycluster`
# to override the ES host `--es.host=127.0.0.1`
# to override the ES port `--es.port=9300`

h1. Usage

The service is now available on http://localhost:8080 with service descriptors from http://localhost:8080/application.wadl

h2. `/health`
This can be used to check if the server is running

h2. `/query`
The `/query` endpoint allows the first n hits and a count plus facets to be returned as JSON.

h3. GET
The `/query` endpoint accepts the following arguments:
- `query` - a JSON-formatted string e.g. `"{"genome":"homo_sapiens","biotype":"protein_coding"}"`
- `fields` - a comma-separated list of fields e.g. `genome,name,description`. `*` can be used as a wildcard e.g. `*` to retrieve all fields
- `sort` - a comma-separated list of fields to sort by, which can be preceded by `+` or `-` to indicate ascending or descending e.g. `+name,-start`
- `limit` - number of hits to return e.g. 100
- `facets` - a comma-separated list of fields to facet over e.g. `facets=genome,biotype`

```http://localhost:8080/query?query={"genome":"nanoarchaeum_equitans_kin4_m"}&limit=5&fields=name,genome&sort=+name,-start&facets=biotype```

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

h3. POST
This is as above, but the parameters are supplied as pure JSON:
```curl -H "Content-Type: application/json" -X POST --data '{"query":{"genome":"nanoarchaeum_equitans_kin4_m"},"limit":10, "fields":["name","genome"],"sort":["+name","-start"]}' http://localhost:8080/query```

h2. `/fetch`
The `/fetch` endpoint is intended for bulk retrieval and accepts the `query`, `fields` and `sort` parameters from `/query`. It can be invoked via GET and POST and returns the results as a JSON array, with no summary or facets available.

h3. GET
```http://localhost:8080/query?query={"genome":"nanoarchaeum_equitans_kin4_m"}&limit=5&fields=name,genome&sort=+name,-start&facets=biotype```

h3. POST
```
curl -H "Content-Type: application/json" -X POST --data '{"query":{"genome":"nanoarchaeum_equitans_kin4_m"},"fields":["name","genome"],"sort":["+name","-start"]}' http://localhost:8080/fetch
```

h2. `/genes`
The `/genes` endpoint is a simple endpoint for retrieving complete documents for genes as JSON.

h3. GET (single gene)
The gene stable ID is supplied as a URL parameter:
```http://localhost:8080/genes/NEQ392```

h3. POST (multiple genes)
A JSON document containing a list of gene stable IDs can be passed via POST:
```curl -H "Content-Type: application/json" -X POST --data '["NEQ392","NEQ393"]' http://localhost:8080/genes```
