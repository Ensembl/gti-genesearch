
```
EG=38 # EG release
E=91 # Ensembl release
N=64 # number of shards
ES_HOST=127.0.0.1
./bin/setup_es.sh http://$ES_HOST:9200/ gene genes_${E}_${EG} 64
./bin/setup_es.sh http://$ES_HOST:9200/ genome genomes_${E}_${EG} 64
./bin/setup_es.sh http://$ES_HOST:9200/ variant variants_${E}_${EG} 64
```