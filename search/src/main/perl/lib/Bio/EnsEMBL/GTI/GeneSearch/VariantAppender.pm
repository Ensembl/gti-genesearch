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

package Bio::EnsEMBL::GTI::GeneSearch::VariantAppender;

use Moose;
use Carp;
use Data::Dumper;
use JSON;
extends 'Bio::EnsEMBL::GTI::GeneSearch::ESClient';
has 'variant_index' => ( is => 'ro', isa => 'Str', required => 1 );

sub get_variation_genomes {
  my ($self) = @_;

  $self->log()->info("Looking for genomes with variation");
  my $results = $self->search()->search(
        index => $self->variant_index(),
        size  => 0,
        body  => {
          aggs => { genomes => { terms => { field => "genome", size => 100 } } }
        } );

  my @genomes =
    map { $_->{key} } @{ $results->{aggregations}{genomes}{buckets} };
  $self->log()->info( "Retrieved " . scalar(@genomes) . " genomes" );
  return \@genomes;
}

sub get_genes {
  my ( $self, $genome ) = @_;
  $self->log()->info("Fetching genes for $genome");

  my $scroll = $self->search()->scroll_helper(
    index => $self->index(),
    body  => {
      query => { term => { genome => $genome } },
      size => 1000,
      _source => ["_id"],
      sort    => '_doc' } );

  $self->log()->info( "Fetching IDs for " . $scroll->total() . " genes" );

  my $genes = [];
  while ( my $doc = $scroll->next() ) {
    push @$genes, $doc->{_id};
  }

  return $genes;
}

sub add_variation_to_transcript {
  my ( $self, $transcript ) = @_;
  # consequence_types, clinical_significance
  $self->log()
    ->info( "Retrieving variants for transcript " . $transcript->{id} );
  my $results = $self->search()->search(
        index => $self->variant_index(),
        body  => {
          _source => [ "locations.annotations", "clinical_significance" ],
          query   => {
            nested => {
              path  => "locations",
              query => {
                nested => {
                  path  => "locations.annotations",
                  query => {
                    term =>
                      { "locations.annotations.stable_id" => $transcript->{id} }
                  } } } } } } );
  my %clinical_significance = ();
  my %consequence_types     = ();
  for my $hit ( @{ $results->{hits}->{hits} } ) {
    if ( defined $hit->{_source}->{clinical_significance} ) {
      %clinical_significance = (
                   %clinical_significance,
                   map { $_ => 1 } %{ $hit->{_source}->{clinical_significance} }
      );
    }
    %consequence_types = ( %consequence_types,
                           map { $_->{name} => 1 }
                             map { @{ $_->{consequences} } }
                             grep { $_->{stable_id} eq $transcript->{id} }
                             map { @{ $_->{annotations} } }
                             @{ $hit->{_source}->{locations} } );
  }
  $transcript->{consequence_types} = [ keys %consequence_types ]
    if %consequence_types;
  $transcript->{clinical_significance} = [ keys %clinical_significance ]
    if %clinical_significance;

  return;
} ## end sub add_variation_to_transcript

sub get_gene {
  my ( $self, $id ) = @_;
  my $results;
  eval {
    $results =
      $self->search()
      ->get( index => $self->index(), type => 'gene', id => $id );
  };
  if ($@) {
    my $msg = "Gene $id not found";
    $self->log()->fatal($msg);
    croak $msg;
  }
  return $results->{_source};
}

sub add_variation_to_gene {
  my ( $self, $gene ) = @_;
  if ( ref($gene) ne 'HASH' ) {
    $gene = $self->get_gene($gene);
  }
  # consequence_types, clinical_significance from transcripts (collected set)
  my %consequence_types     = ();
  my %clinical_significance = ();
  for my $transcript ( @{ $gene->{transcripts} } ) {
    $self->add_variation_to_transcript($transcript);
    %consequence_types = ( %consequence_types,
                           map { $_ => 1 } @{ $transcript->{consequence_types} }
    );
    %clinical_significance = (
                       %clinical_significance,
                       map { $_ => 1 } @{ $transcript->{clinical_significance} }
    );
  }

  if ( %consequence_types || %clinical_significance ) {
    $gene->{consequence_types} = [ keys %consequence_types ]
      if %consequence_types;
    $gene->{clinical_significance} = [ keys %clinical_significance ]
      if %clinical_significance;
    eval {
      $self->bulk()
        ->update( { id => $gene->{id}, doc => $gene, type => 'gene' } );
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
      $self->log()->error($msg);
      croak "Indexing failed: $msg";
    }

  }
  #TODO phenotypes come from a separate index, I think
  #TODO might consider adding this as another reformatted type in ES
  return;
} ## end sub add_variation_to_gene

1;
