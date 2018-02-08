
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

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFile;
use warnings;
use strict;

use base ('Bio::EnsEMBL::Production::Pipeline::Common::Base');

use Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer;

sub run {
  my $self = shift @_;
  my $url = $self->param_required("es_url");
  my $index = $self->param_required("index");
  $self->log()->info("Creating indexer for $index on $url");
  my $indexer =
    Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer->new(
                                url   => $url,
				index => $index
    );
  my $file = $self->param_required('file');
  my $type = $self->param_required('type');
  my $id = $self->param_required('id');
  my $array = $self->param_required('is_array');
  $self->log()->info("Indexing $file as $type");
  $self->log()->debug("id=$id, array=$array");
  $self->dbc()->disconnect_if_idle() if defined $self->dbc();
  $indexer->index_file($file, $type, $id, $array);
  $self->log()->info("Completed indexing $file as $type");
  return;
}

1;
