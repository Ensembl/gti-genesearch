#!/bin/bash
# Copyright [1999-2016] EMBL-European Bioinformatics Institute
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


dir=$(dirname $0)
url=$1
type=$2
n=$3
if [ -z "$n" ]; then
    n=8
fi

echo "Setting up $url $type"


# delete the old index
echo "Deleting old index"
curl -XDELETE "${url}/$type" 
echo
# create a new index
echo "Creating index"
sed -e "s/SHARDN/$n/" ${dir}/../resources/${type}_index.json | curl -XPUT -d @- "${url}/${type}"
