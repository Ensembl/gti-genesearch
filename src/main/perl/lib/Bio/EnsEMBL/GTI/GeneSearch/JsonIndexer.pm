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

has 'url'   => ( is => 'ro', isa => 'Str', required => 1 );
has 'index' => ( is => 'ro', isa => 'Str', default  => 'genes' );
has 'type'  => ( is => 'ro', isa => 'Str', default  => 'gene' );
has 'bulk' => ( is => 'rw', isa => 'Search::Elasticsearch::Bulk' );
has 'timeout' => ( is => 'rw', isa => 'Int', default => 30 );

sub BUILD {
	my ($self) = @_;

	$self->log()->info( "Connecting to " . $self->url() );
	my $es = Search::Elasticsearch->new( nodes           => [ $self->url() ],
										 request_timeout => $self->timeout() );
	my $bulk =
	  $es->bulk_helper( index => $self->index(), type => $self->type() );
	$self->bulk($bulk);
	$self->log()->info( "Connected to " . $self->url() );
	return;
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
			$self->log()
			  ->error( $@->{type} . " error: " .
					   $@->{text} );
		}
		else {
			$self->log()->error( "Indexing failed: " . $@ );
		}
		croak "Indexing $file failed";
	}
	$self->log()->info("Completed loading $n entries from $file");
	return;
} ## end sub index_file
1;
