#!/bin/bash

dir=$(dirname $0)
url=$1
n=$2
if [ -z "$n" ]; then
    n=8
fi
echo "Setting up $url"
# delete the old index
echo "Deleting old index"
curl -XDELETE "${url}/" 
echo
# create a new index
echo "Creating index"
# disable replicas
curl -XPUT -d "{\"number_of_shards\": $n, \"number_of_replicas\" : 0}" "${url}/"
echo

## genome mapping - ignore for now
#echo "Loading genome mapping"
#curl -XPUT -d @${dir}/genome_mapping.json "${url}/_mapping/genome"

# gene mapping
echo "Loading gene mapping"
curl -XPUT -d @${dir}/gene_mapping.json "${url}/_mapping/gene" 
echo
