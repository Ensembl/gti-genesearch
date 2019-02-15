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

use HTTP::Request;
use LWP::UserAgent;
use Data::Dumper;
use Getopt::Long;
use File::Slurp;
use File::Glob;
use Carp;
use Time::HiRes qw(gettimeofday);
use Log::Log4perl qw(:easy);
use JSON;
use List::Util qw(min max);
use Statistics::Basic qw(:all);

my $lwp = LWP::UserAgent->new();
$lwp->timeout(600);

my $opts = {};
GetOptions( $opts,       'query=s@',
            'action:s',  'fields=s@',
            'facets=s@', 'sort=s@',
            'uri:s',     'limit:s',
            'verbose',   'iterations:s',
            'es_uri:s',  'outfile:s',
            'warm',      'nocache',
            'write_results' );

if ( $opts->{verbose} ) {
  Log::Log4perl->easy_init($DEBUG);
}
else {
  Log::Log4perl->easy_init($INFO);
}
my $logger = Log::Log4perl->get_logger();

$opts->{uri}        ||= 'http://gti-es-0.ebi.ac.uk:8080/api/genes';
$opts->{es_uri}     ||= 'http://gti-es-0.ebi.ac.uk:9200';
$opts->{action}     ||= 'query';
$opts->{iterations} ||= 5;
$opts->{limit}      ||= 100;
$opts->{fields}     ||= [ 'id', 'name', 'description', 'genome' ];
$opts->{facets}     ||= [];
$opts->{sort}       ||= [];
$opts->{outfile}    ||= 'query.md';

open my $outfile, ">", $opts->{outfile} or
  croak "Could not open $opts->{outfile}";
print $outfile
  "|Query|Time (s)|Min|Max|Std dev|\n|:---|---:|---:|---:|---:|\n";
for my $query ( @{ $opts->{query} } ) {
  for my $query_file ( glob($query) ) {
    $logger->info("Processing $query_file");
    my $query = from_json( read_file($query_file) );
    my @times = ();
    for ( my $i = 1; $i <= $opts->{iterations}; $i++ ) {
      $logger->debug("Iteration $i/$opts->{iterations}");
      clear_cache( $opts->{es_uri} ) unless ( $opts->{nocache} );
      warm($query) if ( $opts->{warm} );
      my $res_file;
      if ( defined $opts->{write_results} ) {
        ( $res_file = $query_file ) =~ s/.json/.results/;
      }
      if ( $opts->{action} eq 'query' ) {
        push @times,
          execute_post( "$opts->{uri}/query", {
                          query  => $query,
                          limit  => $opts->{limit},
                          fields => $opts->{fields},
                          sort   => $opts->{sort},
                          facets => $opts->{facets} },
                        $res_file );

      }
      elsif ( $opts->{action} eq 'fetch' ) {
        push @times,
          execute_post( "$opts->{uri}/fetch",
                        { query => $query, fields => $opts->{fields} },
                        $res_file );

      }
      else {
        croak "Action " . $opts->{action} . " not supported";
      }
    } ## end for ( my $i = 1; $i <= ...)
    my $mean_time = mean(@times);
    my $min       = min(@times);
    my $max       = max(@times);
    my $stddev    = stddev(@times);
    $logger->info(
       $query_file . " took " . $mean_time . " s ($min $max $stddev)" );
    printf $outfile "|%s|%.2f|%.2f|%.2f|%.2f|\n", $query_file,
      $mean_time, $min, $max, $stddev;
  } ## end for my $query_file ( glob...)
} ## end for my $query ( @{ $opts...})
close $outfile;

sub clear_cache {
  my ($uri) = @_;
  $logger->debug("Clearing cache");
  my $req = HTTP::Request->new( 'POST', $uri . '/genes/_cache/clear' );
  $lwp->request($req);
  return;
}

sub warm {
  my ($query) = @_;
  my $warm_query;
  if ( defined $query->{genome} ) {
    $warm_query->{genome} = $query->{genome};
  }
  if ( defined $query->{lineage} ) {
    $warm_query->{lineage} = $query->{lineage};
  }
  if ( defined $warm_query ) {
    $logger->debug( "Warming query: " . Dumper($warm_query) );
    execute_post( "$opts->{uri}/query",
                  { query => $warm_query, limit => 0, fields => [] } );
  }
  return;
}

sub execute_post {
  my ( $uri, $payload, $outfile ) = @_;
  $logger->debug("POSTing to $uri");
  my $req = HTTP::Request->new( 'POST', $uri );
  $req->header( 'Content-Type' => 'application/json' );
  $req->content( to_json($payload) );
  my $start_time = gettimeofday();
  my $response   = $lwp->request($req);
  my $time       = gettimeofday() - $start_time;
  if ( !$response->is_success() ) {
    croak $response->status_line();
  }
  if ( defined $outfile ) {
    write_file( $outfile, $response->content() );
  }
  $logger->debug( "Took " . $time );
  return $time;
}
