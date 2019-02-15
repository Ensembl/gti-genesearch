
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

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::GeneFactory;
use warnings;
use strict;
use base ('Bio::EnsEMBL::Production::Pipeline::Common::Base');
use List::MoreUtils qw/natatime/;

use Bio::EnsEMBL::GTI::GeneSearch::VariantAppender;

sub run {
  my $self = shift @_;
  $self->log("Finding genes for genomes");
  my $indexer =
    Bio::EnsEMBL::GTI::GeneSearch::VariantAppender->new(
                         url           => $self->param_required("es_url"),
                         index         => $self->param_required('index'),
                         variant_index => $self->param_required('variant_index')
    );
  my $genes = $indexer->get_genes( $self->param_required('genome') );
  $self->log->info( "Retrieved " . scalar(@$genes) . " genes" );
  my $it = natatime( 1000, @$genes );
  while ( my @ids = $it->() ) {
    $self->log->info("Flowing ".scalar(@ids)." gene IDs");
    $self->dataflow_output_id( { gene_ids => \@ids }, 2 );
  }
  return;
}

1;
