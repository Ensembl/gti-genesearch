#!/bin/env perl
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
use warnings;
use strict;

use JSON;
use File::Slurp;
use Log::Log4perl qw/:easy/;
use Getopt::Long;
use Data::Dumper;
use Carp;

my $opts = {};
GetOptions ($opts, 'verbose', 'mapping_file|f:s', 'xref|x:s', 'target|t:s');

if(!$opts->{mapping_file}) {
    pod2usage("Mapping file not specified");
}

if(!$opts->{xref}) {
    pod2usage("Xref type not specified");
}

if(!$opts->{target}) {
    pod2usage("Target not specified");
}

if ( $opts->{verbose} ) {
  Log::Log4perl->easy_init($DEBUG);
}
else {
  Log::Log4perl->easy_init($INFO);
}
my $logger =  Log::Log4perl->get_logger();

$logger->info("Reading from $opts->{mapping_file}");
my $mapping = from_json(read_file($opts->{mapping_file}));

if($opts->{xref} =~ m/[^A-z0-9_]+/) {
  die "Xref name cannot contain non-alphanumeric/underscores";
}

if($opts->{target} eq 'gene' || $opts->{target} eq 'transcript' ||
   $opts->{target} eq 'translation') {
    $logger->info("Adding $opts->{xref} to gene");
    my $gene_mapping = $mapping->{gene}{properties};
    add_xref($gene_mapping,$opts->{xref});
    
    if($opts->{target} eq 'transcript' ||
       $opts->{target} eq 'translation') {

            $logger->info("Adding $opts->{xref} to transcript");        
        my $transcript_mapping = $gene_mapping->{transcripts}{properties};
        add_xref($transcript_mapping,$opts->{xref});
                 
        if($opts->{target} eq 'translation') {
            $logger->info("Adding $opts->{xref} to translation");
            my $translation_mapping = $transcript_mapping->{translations}{properties};
            add_xref($translation_mapping,$opts->{xref});
        }               
    }
    $logger->info("Writing to $opts->{mapping_file}");
    write_file($opts->{mapping_file}, to_json($mapping,{pretty=>1}));
} else {
    croak "Unknown object type $opts->{target}";
}

sub add_xref {
    my ($target, $xref) = @_;
    if(!defined $xref) {
        die "No xref";
    }
    if(!$target->{$xref}) {
        $target->{$xref} = {                      
            'norms' => {
                'enabled' => 'false'                
            },
                    'type' => 'string',
                    'index' => 'not_analyzed'
        };
    }
    return;
}
