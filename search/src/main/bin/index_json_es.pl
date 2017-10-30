#!/bin/env perl
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

use warnings;
use strict;

use JSON;
use Search::Elasticsearch;
use Log::Log4perl qw/:easy/;
use Getopt::Long;
use Carp;
use Pod::Usage;
use File::Slurp;

my $opts = {};
GetOptions ($opts, 'verbose', 'genes_file:s@', 'genome_file:s@', 'es_url:s','index:s');

if(!$opts->{es_url}) {
    pod2usage("ElasticSearch URL not specified");
}

if(!$opts->{index}) {
    pod2usage("Index name not specified");
}

if ( $opts->{verbose} ) {
  Log::Log4perl->easy_init($DEBUG);
}
else {
  Log::Log4perl->easy_init($INFO);
}
my $logger = get_logger();

$logger->info("Connecting to ES instance on $opts->{es_url}");
my $es = Search::Elasticsearch->new(nodes=>[$opts->{es_url}]);

{
  my $bulk = $es->bulk_helper(
                              index   => $opts->{index},
                              type    => 'genome'
                             );
  
  for my $file (@{$opts->{genome_file}}) {
    $logger->info("Loading from $file");
    my $genome = from_json(read_file($file));
    $logger->debug("Loading $genome->{id}");
    $bulk->index({id=>$genome->{id}, source=>$genome});
    $logger->info("Completed loading genome from $file");
  }
  $bulk->flush();
}


{
  my $bulk = $es->bulk_helper(
                              index   => $opts->{index},
                              type    => 'gene'
                             );
  
  for my $file (@{$opts->{genes_file}}) {
    $logger->info("Loading from $file");
    my $n = 0;
    my $genes = from_json(read_file($file));
    for my $gene (@$genes) {
      $n++;
      $logger->debug("Loading $gene->{id}");
      $bulk->index({id=>$gene->{id}, source=>$gene});
    }
    $logger->info("Completed loading $n entries from $file");
  }
  $bulk->flush();
}



