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

=cut

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::UpdateVariation;
use warnings;
use strict;

use base ('Bio::EnsEMBL::Production::Pipeline::Common::Base');
use Bio::EnsEMBL::GTI::GeneSearch::VariantAppender;
use Data::Dumper;

sub run {
    my $self = shift @_;

    my $indexer =
        Bio::EnsEMBL::GTI::GeneSearch::VariantAppender->new(
            url           => $self->param_required("es_url"),
            index         => $self->param_required('index'),
            variant_index => $self->param_required('variant_index')
        );

    # gene IDs
    my $gene_ids = $self->param_required("gene_ids");

    for my $id (@$gene_ids) {
        $self->log->info("Updating gene $id");
        $indexer->add_variation_to_gene($id);
    }
    $indexer->bulk()->flush();

    return;

} ## end sub run

1;
