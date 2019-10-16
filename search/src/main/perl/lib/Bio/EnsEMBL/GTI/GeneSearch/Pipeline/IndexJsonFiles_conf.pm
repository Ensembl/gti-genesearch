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

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFiles_conf;

use strict;
use warnings;

use base ('Bio::EnsEMBL::Hive::PipeConfig::HiveGeneric_conf');

sub resource_classes {
    my ($self) = @_;
    return {
        'default' => { 'LSF' => '-q production-rh74' }
    };
}

sub default_options {
    my ($self) = @_;
    return { %{$self->SUPER::default_options()},
        dumps_dir      => '.',
        blacklist      => undef,
        es_url         => 'http://127.0.0.1:9200/',
        genes_index    => 'genes',
        genomes_index  => 'genomes',
        variants_index => 'variants'
    };
}

sub pipeline_analyses {
    my ($self) = @_;
    return [
        {
            -logic_name  => 'JsonFileFactory',
            -module      => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::JsonFileFactory',
            -meadow_type => 'LOCAL',
            -input_ids   => [ { dumps_dir => $self->o('dumps_dir') } ],
            -parameters  => { blacklist => $self->o('blacklist') },
            -flow_into   => {
                '2' => [ 'IndexGenomeJsonFile' ],
                '3' => [ 'IndexGenesJsonFile' ],
                '4' => [ 'IndexVariantsJsonFile' ]
            }
        },
        {
            -logic_name    => 'IndexGenomeJsonFile',
            -module        => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFile',
            -meadow_type   => 'LSF',
            -hive_capacity => 10,
            -rc_name       => 'default',
            -parameters    => {
                'es_url'   => $self->o('es_url'),
                'index'    => $self->o('genomes_index'),
                'type'     => 'genome',
                'id'       => 'id',
                'is_array' => 0
            }
        },
        {
            -logic_name    => 'IndexGenesJsonFile',
            -module        => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFile',
            -meadow_type   => 'LSF',
            -hive_capacity => 10,
            -rc_name       => 'default',
            -parameters    => {
                'es_url'   => $self->o('es_url'),
                'index'    => $self->o('genes_index'),
                'type'     => 'gene',
                'id'       => 'id',
                'is_array' => 1
            }
        },
        {
            -logic_name    => 'IndexVariantsJsonFile',
            -module        => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFile',
            -meadow_type   => 'LSF',
            -hive_capacity => 10,
            -rc_name       => 'default',
            -parameters    => {
                'es_url'   => $self->o('es_url'),
                'index'    => $self->o('variants_index'),
                'type'     => 'variant',
                'id'       => 'id',
                'is_array' => 1
            }
        }
    ];
} ## end sub pipeline_analyses

1;
