# Manual deployment

To build and run:
```console
./gradlew clean :web:bootRepackage
# launch - optional command line options shown
java -Xmx2g -jar web/build/libs/web-0.1.jar --es.node=false --es.host=gti-es-0 --rest.url.eg=http://rest.ensemblgenomes.org/sequence/id --rest.url.ens=http://rest.ensembl.org/sequence/id
```
* Note 1: JDK 9 isn't currently compatible with SpringBoot. Make sure your JDK version is < 9.
* Note 2: Use of Lets Encrypt-based sites such as EBiSC require 8u101 or better.*

# Configuration

As you might expect for a Spring boot app, configuration is typically carried out using the `application.properties` file but can be passed to the java command line used to start Spring Boot. This supports all the wonderful things that Spring Boot does so well, but here are some useful config properties.

General Spring boot options:
* `server.port` - port to run Spring boot on (default is good old `8080`)
* `spring.jersey.applicationPath` - path for REST API (`/api` by default)
* `security.basic.enabled` - set to `false` to disable basic authentication. Otherwise, basic auth will be on!
* `security.user.password` - configure a password for basic auth
* `spring.profiles.active` - variation profile to use (`default`, `ebisc`, `eva_rest`, `ensembl_rest`, `eva_mongo`)
* `debug` - set to `true` for lots of debuggy goodness

SSL support options (https requires a key store):
* `server.ssl.key-store` - path to key store
* `server.ssl.key-store-password` - password for key store
* `server.ssl.keyAlias` - alias for key to use
* `server.ssl.keyStoreType` - key store type (default is `PKCS12`)

Ensembl data options:
* `es.host` - head node of Elastic cluster
* `es.port` - port of Elastic cluster head node (default is `9300`)
* `es.node` - if `true`, join cluster as node (rather as transport client)
* `es.cluster` - name of elastic cluster
* `rest.url.eg` - base URL for EG REST
* `rest.url.ens` - base URL for Ensembl REST

EVA-related options (depending if an EVA profile is used):
* `eva.rest.url`
* `mongo.url`
* `mongo.database`
* `mongo.collection`

GXA Solr instance options:
* `solr.expression.url`
* `solr.experiments.url`

EBiSC-related options (for `ebisc` profile):
* `ebisc.rest.url` - EBiSC metadata REST url
* `ebisc.rest.username`,  `ebisc.rest.api_token` - credentials for EBiSC metadata REST
* `ebisc.ega.url` - htsget REST API url
* `ebisc.ega.accession` - single file accession to use (contains all EBiSC data)

# Deployment with ansible

To make life easier (at least until we get proper Kubernetes support!), there are ansible files allowing a cluster of Elastic nodes and a web app to be automatically provisioned. Find out more about deployment [here](../../deployment/README.md) 