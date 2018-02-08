
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

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::AddVariation_conf;

use strict;
use warnings;

use base ('Bio::EnsEMBL::Hive::PipeConfig::HiveGeneric_conf');

sub resource_classes {
  my ($self) = @_;
  return {
    'default' => { 'LSF' => '-q production-rh6' },
    'himem' => { 'LSF' => '-q production-rh6 -M  16000 -R "rusage[mem=16000]"' }
  };
}

sub default_options {
  my ($self) = @_;
  return { %{ $self->SUPER::default_options() },
           es_url        => 'http://gti-es-0.ebi.ac.uk:9200/',
           index         => 'genes',
           variant_index => 'variants' };
}

sub pipeline_analyses {
  my ($self) = @_;
  return [ {
      -logic_name => 'VariationFactory',
      -module => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::VariationFactory',
      -meadow_type => 'LOCAL',
      -input_ids   => [ {} ],
      -parameters  => {es_url        => $self->o('es_url'),
                       index         => $self->o('index'),
                       variant_index => $self->o('variant_index') },
      -flow_into => { '2' => ['GeneFactory'] } }, {
      -logic_name  => 'GeneFactory',
      -module      => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::GeneFactory',
      -meadow_type => 'LOCAL',
      -parameters  => {
                 es_url        => $self->o('es_url'),
                 index         => $self->o('index'),
                 variant_index => $self->o('variant_index')
      },
      -flow_into => { '2' => ['UpdateVariation'] } },

    { -logic_name => 'UpdateVariation',
      -module     => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::UpdateVariation',
      -hive_capacity => 25,
      -rc_name       => 'default',
      -flow_into     => { -1 => 'UpdateVariationHiMem', },
      -parameters    => {
                       'es_url'      => $self->o('es_url'),
                       index         => $self->o('index'),
                       variant_index => $self->o('variant_index') } }, {
      -logic_name => 'UpdateVariationHiMem',
      -module     => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::UpdateVariation',
      -hive_capacity => 25,
      -rc_name       => 'himem',
      -parameters    => {
                       'es_url'      => $self->o('es_url'),
                       index         => $self->o('index'),
                       variant_index => $self->o('variant_index') } } ];
} ## end sub pipeline_analyses

1;
