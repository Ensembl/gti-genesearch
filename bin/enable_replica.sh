#!/bin/bash

dir=$(dirname $0)
url=$1
echo "Setting up $url"
curl -XPUT -d '{"index" : {"number_of_replicas" : 1}}' "${url}/_mapping/gene" 
