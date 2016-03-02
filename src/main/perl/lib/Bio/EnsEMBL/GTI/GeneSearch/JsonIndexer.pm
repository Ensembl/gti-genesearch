#!/bin/env perl
package Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer;

use warnings;
use strict;
use Moose;
with 'MooseX::Log::Log4perl';

use Search::Elasticsearch;
use File::Slurp;
use JSON;
use Carp;
use EGExt::FTP::JSON::JsonRemodeller;

has 'url'   => ( is => 'ro', isa => 'Str', required => 1 );
has 'index' => ( is => 'ro', isa => 'Str', default  => 'genes' );
has 'type'  => ( is => 'ro', isa => 'Str', default  => 'gene' );
has 'bulk' => ( is => 'rw', isa => 'Search::Elasticsearch::Bulk' );
has 'timeout' => ( is => 'rw', isa => 'Int', default => 30 );

sub BUILD {
  my ($self) = @_;

  $self->log()->info( "Connecting to " . $self->url() );
  $self->{es} =
    Search::Elasticsearch->new( nodes           => [ $self->url() ],
                                request_timeout => $self->timeout() );
  my $bulk =
    $self->{es}->bulk_helper( index => $self->index(), type => $self->type() );
  $self->bulk($bulk);
  $self->log()->info( "Connected to " . $self->url() );
  return;
}

sub search {
  my ($self) = @_;
  return $self->{es};
}

sub index_file {
  my ( $self, $file ) = @_;
  $self->log()->info("Loading from $file");
  my $n     = 0;
  my $genes = from_json( read_file($file) );
  eval {
    for my $gene (@$genes)
    {
      $n++;
      $self->log()->debug("Loading $gene->{id}");
      $self->bulk()->index( { id => $gene->{id}, source => $gene } );
    }
    $self->bulk()->flush();
  };
  if ($@) {
    # parse out error from rest of body
    if ( $@->isa('Search::Elasticsearch::Error') ) {
      $self->log()->error( $@->{type} . " error: " . $@->{text} );
    }
    else {
      $self->log()->error( "Indexing failed: " . $@ );
    }
    croak "Indexing $file failed";
  }
  $self->log()->info("Completed loading $n entries from $file");
  return;
} ## end sub index_file

sub fetch_genes {
  my ( $self, $gene_ids ) = @_;

  # retrieve gene documents matching the provided genes
  my $transcript_docs = {};
  my $gene_docs       = [];
  my $result =
    $self->{es}
    ->mget( index => 'genes', type => 'gene', body => { ids => $gene_ids } );

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
    $self->bulk()
      ->index(
        { index => 'genes', type => 'gene', id => $gene->{id}, source => $gene } )
      ;
  }
  $self->bulk()->flush();
  return;
}

1;
