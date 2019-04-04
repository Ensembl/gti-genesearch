=head1 LICENSE

.. See the NOTICE file distributed with this work for additional information
   regarding copyright ownership.
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

=cutt

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::VariationFactory;
use warnings;
use strict;
use base ('Bio::EnsEMBL::Production::Pipeline::Common::Base');

use Bio::EnsEMBL::GTI::GeneSearch::VariantAppender;

sub run {
  my $self = shift @_;
  $self->log("Finding genomes with variants");
  my $indexer =
    Bio::EnsEMBL::GTI::GeneSearch::VariantAppender->new(
                         url           => $self->param_required("es_url"),
                         index         => $self->param_required('index'),
                         variant_index => $self->param_required('variant_index')
    );
  my $genomes = $indexer->get_variation_genomes();
  # flow 1 job per genome
  my $genome_jobs = [ map { { genome => $_ } } @$genomes ];
  $self->dataflow_output_id( $genome_jobs, 2 );
  return;
}

1;
