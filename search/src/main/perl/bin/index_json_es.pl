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
use Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer;

my $opts = {};
GetOptions ($opts, 'verbose', 'file:s@', 'es_url:s', 'type:s', 'id:s', 'offset:i', 'limit:i');

if(!$opts->{es_url}) {
    pod2usage("ElasticSearch URL not specified");
}
croak "Type not specified" unless defined $opts->{type};
croak "File not specified" unless defined $opts->{file};
if ( $opts->{verbose} ) {
  Log::Log4perl->easy_init($DEBUG);
}
else {
  Log::Log4perl->easy_init($INFO);
}
my $logger = get_logger();
my $indexer = Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer->new(url => $opts->{es_url});
$opts->{id} ||= 'id';

for my $file (@{$opts->{file}}) {
    $logger->info("Indexing $file");
    my $array = $opts->{type} eq 'genome'?0:1;
    $indexer->index_file($file, $opts->{type}, $opts->{id}, $array, $opts->{offset}, $opts->{limit});
}
