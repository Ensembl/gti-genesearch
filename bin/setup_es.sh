#!/bin/bash

dir=$(dirname $0)
url=$1
echo "Setting up $url"
# delete the old index
echo "Deleting old index"
curl -XDELETE "${url}/" 
# create a new index
echo "Creating index"
curl -XPUT "${url}/"
## genome mapping - ignore for now
#echo "Loading genome mapping"
#curl -XPUT -d @${dir}/genome_mapping.json "${url}/_mapping/genome"

# gene mapping
echo "Loading gene mapping"
curl -XPUT -d @${dir}/gene_mapping.json "${url}/_mapping/gene" 
