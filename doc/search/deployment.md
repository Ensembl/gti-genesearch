# Building 

This part of the gti-genesearch package is intended to be used as an API by other applications using gradle for dependency management, but an application distribution can be built that contains an example command line application:

To build the example application:
```console
./gradlew clean :search:distTar
tar xvf search/build/distributions/search-0.2.tar
cd search-0.2
./bin/search -help # show the options
```

# Deployment with ansible

To make life easier (at least until we get proper Kubernetes support!), there are ansible files allowing a cluster of Elastic nodes and a web app to be automatically provisioned. Find out more about deployment [here](../../deployment/README.md) 