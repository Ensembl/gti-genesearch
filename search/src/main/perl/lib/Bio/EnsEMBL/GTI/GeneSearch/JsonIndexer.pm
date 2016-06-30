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

use warnings;
use strict;
use Moose;
with 'MooseX::Log::Log4perl';

use Search::Elasticsearch;
use File::Slurp;
use JSON;
use Carp;
use Data::Dumper;

has 'url'         => ( is => 'ro', isa => 'Str', required => 1 );
has 'index'       => ( is => 'ro', isa => 'Str', default  => 'genes' );
has 'gene_type'   => ( is => 'ro', isa => 'Str', default  => 'gene' );
has 'genome_type' => ( is => 'ro', isa => 'Str', default  => 'genome' );
has 'bulk' => ( is => 'rw', isa => 'Search::Elasticsearch::Bulk' );
has 'timeout' => ( is => 'rw', isa => 'Int', default => 300 );

sub BUILD {
  my ($self) = @_;

  $self->log()->info( "Connecting to " . $self->url() );
  $self->{es} =
    Search::Elasticsearch->new( nodes           => [ $self->url() ],
                                request_timeout => $self->timeout() );
  my $bulk = $self->{es}->bulk_helper(
    index    => $self->index(),
    on_error => sub {
      my ( $action, $response, $i ) = @_;
      $self->handle_error( $action, $response, $i );
    } );
<<<<<<< HEAD

=======
>>>>>>> acc01b3... catch any indexing errors
  $self->bulk($bulk);
  $self->log()->info( "Connected to " . $self->url() );
  return;
}

sub search {
  my ($self) = @_;
  return $self->{es};
}

sub handle_error {
  my ( $self, $action, $response, $i ) = @_;
  croak "Failed to execute $action $i due to " .
    $response->{error}->{type} . " error: " .
    $response->{error}->{reason};
  return;
}

sub index_file {
  my ( $self, $file ) = @_;
  $self->log()->info("Loading from $file");
  my $n      = 0;
  my $genome = from_json( read_file($file) );
<<<<<<< HEAD
  $genome->{name} = $genome->{id};
=======
>>>>>>> acc01b3... catch any indexing errors

  eval {
    for my $gene ( @{ $genome->{genes} } )
    {
      $n++;
      $gene->{genome} = $genome->{id};
      $self->log()->debug("Loading $gene->{id}");
      $self->bulk()->index( { id     => $gene->{id},
                              source => $gene,
                              , type => $self->gene_type() } );
    }
    $self->bulk()->flush();
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
    croak "Indexing $file failed: $msg";
  }
  $self->log()->info("Completed loading $n entries from $file");
  $self->log()->info("Indexing genome");
  delete $genome->{genes};
  $self->bulk()->index( { id     => $genome->{id},
                          source => $genome,
                          type   => $self->genome_type() } );
  $self->bulk()->flush();
  $self->log()->info("Completed indexing genome");
  return;
} ## end sub index_file

sub fetch_genes {
  my ( $self, $gene_ids ) = @_;

  # retrieve gene documents matching the provided genes
  my $transcript_docs = {};
  my $gene_docs       = [];
  my $result = $self->{es}->mget( index => $self->index(),
                                  type  => $self->gene_type(),
                                  body  => { ids => $gene_ids } );

  # create an ID to transcript hash
  for my $gene_doc ( @{ $result->{docs} } ) {
    push @$gene_docs, $gene_doc->{_source};
  }

  return $gene_docs;
}

sub index_genes {
  my ( $self, $gene_docs ) = @_;
  # reindex
  for my $gene ( @{$gene_docs} ) {
    $self->bulk()->index( { index  => $self->index(),
                            type   => $self->gene_type(),
                            id     => $gene->{id},
                            source => $gene } );
  }
  $self->bulk()->flush();
  return;
}

1;
