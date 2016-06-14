
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

use base ('Bio::EnsEMBL::Hive::Process');

use Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer;
use Log::Log4perl qw(:easy);
Log::Log4perl->easy_init($INFO);

sub fetch_input {
  my ($self) = @_;
  $self->{indexer} =
    Bio::EnsEMBL::GTI::GeneSearch::JsonIndexer->new(
                                index => $self->param_required("index"),
                                url   => $self->param_required("es_url")
    );
  return;
}

sub run {
  my $self = shift @_;
  my $file = $self->param_required('file');
  $self->{indexer}->index_file($file);
  return;
}

1;
