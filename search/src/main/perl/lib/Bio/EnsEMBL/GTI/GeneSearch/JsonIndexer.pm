#!/bin/env perl

=head1 LICENSE

Copyright [1999-2016] EMBL-European Bioinformatics Institute

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

=cut

package Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer;

use File::Slurp;
use JSON;
use Carp;
use Data::Dumper;

use Moose;
extends 'Bio::EnsEMBL::GTI::GeneSearch::ESClient';
with 'MooseX::Log::Log4perl';


use Bio::EnsEMBL::Production::Search::JSONReformatter qw/process_json_file/;

sub index_file {
  my ( $self, $file, $type, $id_field, $array, $offset, $limit ) = @_;
  if ( $array == 1 ) {
    $self->index_file_array( $file, $type, $id_field, $offset, $limit );
  }
  else {
    $self->index_file_single( $file, $type, $id_field );
  }
  return;
}

sub index_file_single {
  my ( $self, $file, $type, $id_field ) = @_;
  $self->log->info("Loading from $file");
  $self->index_document( from_json( read_file($file) ), $type, $id_field );
  $self->bulk()->flush();
  return;
}

sub index_file_array {
  my ( $self, $file, $type, $id_field, $offset, $limit ) = @_;
  $self->log->info("Loading array from $file");
      if(defined $offset && defined $limit) {
  $self->log->info("Offset=$offset, limit=$limit");
      }
  my $n = 0;
  my $m = 0;
  process_json_file(
    $file,
    sub {
      my $doc = shift;
      $n++;
      if(defined $offset && defined $limit) {
        return if($n<$offset+1 || $n>($offset+$limit));
      }
      $m++;
      $self->index_document( $doc, $type, $id_field );
      return;
    } );
  $self->bulk()->flush();
  $self->log->info("Loaded $m/$n $type documents from $file");
  return;
}

sub index_document {
  my ( $self, $doc, $type, $id ) = @_;
  eval {
    $self->bulk()->create( { id => $doc->{$id}, source => $doc, type => $type } );
  };
  if ($@) {
    my $msg;
    # parse out error from rest of body
    if ( $@->isa('Search::Elasticsearch::Error') ) {
      $msg = $@->{type} . " error: " . $@->{text};
    }
    else {
      $msg = "Indexing failed: " . $@;
    }
    $self->log->error($msg);
    croak "Indexing failed: $msg";
  }
  return;
}
1;
