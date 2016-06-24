
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

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFiles_conf;

use strict;
use warnings;

use base ('Bio::EnsEMBL::Hive::PipeConfig::HiveGeneric_conf');

sub resource_classes {
  my ($self) = @_;
  return {
     'default' => { 'LSF' => '-q production-rh6' },
     'himem' =>
       { 'LSF' => '-q production-rh6 -M  32000 -R "rusage[mem=32000]"' }
  };
}

sub default_options {
  my ($self) = @_;
  return { %{ $self->SUPER::default_options() },
           dumps_dir => '.',
           blacklist => undef,
           es_url    => 'http://127.0.0.1:9200/',
           es_index  => 'genes' };
}

sub pipeline_analyses {
  my ($self) = @_;
  return [ {
           -logic_name => 'JsonFileFactory',
           -module =>
             'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::JsonFileFactory',
           -meadow_type => 'LOCAL',
           -input_ids   => [ { dumps_dir => $self->o('dumps_dir') } ],
           -parameters => { blacklist => $self->o('blacklist') },
           -flow_into  => { '1'       => ['IndexJsonFile'] } }, {
           -logic_name => 'IndexJsonFile',
           -module =>
             'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFile',
           -meadow_type   => 'LSF',
           -hive_capacity => 10,
           -rc_name       => 'default',
           -flow_into     => { -1 => 'IndexJsonFileHiMem', },
           -parameters    => {
                            'es_url' => $self->o('es_url'),
                            'index'  => $self->o('index') } }, {
           -logic_name => 'IndexJsonFileHiMem',
           -module =>
             'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFile',
           -hive_capacity => 10,
           -rc_name       => 'himem',
           -parameters    => {
                            'es_url' => $self->o('es_url'),
                            'index'  => $self->o('index') } } ];
} ## end sub pipeline_analyses

1;
