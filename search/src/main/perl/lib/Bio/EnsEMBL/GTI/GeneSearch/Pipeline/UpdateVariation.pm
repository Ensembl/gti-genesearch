
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

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::UpdateVariation;
use warnings;
use strict;

use base ('Bio::EnsEMBL::Hive::Process');
use Bio::EnsEMBL::Production::Pipeline::JSON::JsonRemodeller;
use Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer;
use Bio::EnsEMBL::Registry;
use Data::Dumper;
use Log::Log4perl qw(:easy);

Log::Log4perl->easy_init($INFO);

sub fetch_input {
  my ($self) = @_;
  $self->{indexer} =
    Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer->new(
                               url => $self->param_required("es_url") );
  # species
  $self->{variation_dba} =
    Bio::EnsEMBL::Registry->get_DBAdaptor(
                                       $self->param_required("species"),
                                       "variation" );
  # use streaming to make retrieving the result faster
  $self->{variation_dba}->dbc()->db_handle()->{mysql_use_result} = 1;

  # gene IDs
  $self->{gene_ids} = $self->param_required("gene_ids");

  $self->{logger} = get_logger();

  return;
}

sub log {
  my ($self) = @_;
  return $self->{logger};
}

sub run {
  my $self = shift @_;

  # retrieve gene documents matching the provided genes
  my $result =
    $self->{indexer}->search()->mget(
                 index => 'genes',
                 type  => 'gene',
                 body => { ids => $self->{gene_ids}, _source => "true" }
    );

  # create an ID to transcript hash
  my @gene_docs =
    map { $_->{_source} } grep { $_->{found} } @{ $result->{docs} };
  $self->log()
    ->info( "Retrieved " . scalar @gene_docs . " genes for update" );

  my $remodeller =
    Bio::EnsEMBL::Production::Pipeline::JSON::JsonRemodeller->new(
                             -variation_dba => $self->{variation_dba} );

  my $updated_genes = $remodeller->add_variation( \@gene_docs );
  $self->log()->info( "Updated " . scalar @$updated_genes . " genes" );

  $self->{indexer}->index_genes($updated_genes);
  $self->log()
    ->info( "Finished indexing " . scalar @$updated_genes . " genes" );

  return;

} ## end sub run

1;
