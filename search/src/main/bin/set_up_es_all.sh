#!/bin/bash
# ..  See the NOTICE file distributed with this work for additional information
#     regarding copyright ownership.
#     Licensed under the Apache License, Version 2.0 (the "License");
#     you may not use this file except in compliance with the License.
#     You may obtain a copy of the License at
#       http://www.apache.org/licenses/LICENSE-2.0
#     Unless required by applicable law or agreed to in writing, software
#     distributed under the License is distributed on an "AS IS" BASIS,
#     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#     See the License for the specific language governing permissions and
#     limitations under the License.#
#


dir=$(dirname $0)
url=$1
release=$2
n=$3
r=$4
if [[ -z "$n" ]]; then
    n=8
fi

if [[ -z "$r" ]]; then
    n=0
fi

if [[ -z "$url" ]] ||  [[ -z "$release" ]] ; then
    echo "Usage: $0 <url> <release> [shardN] [Replicas]" 1>&2
    exit 1
fi

echo "Setting up $url $type $name"

# delete the old index
# create a new index
resources=${dir}/../resources/indexes/
for filename in `ls ${resources}`; do
  echo $filename | rev | cut -d'_' -f2- | rev
  type=`echo ${filename} | rev | cut -d'_' -f2- | rev`
  echo "Type ${type}"
  name="${type}s"
  echo "Deleting old index ${name}"
  curl -XDELETE "${url}/${name}"
  echo
  echo "Creating index "
  sed -e "s/SHARDN/$n/" -e "s/REPLICAS/$r/" ${dir}/../resources/indexes/${type}_index.json | curl -XPUT -d @- -H "Content-Type: application/json" "${url}/${name}"
done

