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

sub beekeeper_extra_cmdline_options {
  my ($self) = @_;
  my $opts = '-reg_conf '.$self->o('registry');
  return $opts;
}

sub default_options {
  my ($self) = @_;
  return { %{ $self->SUPER::default_options() },
           es_url    => 'http://gti-es-0.ebi.ac.uk:9200/' };
}

sub pipeline_analyses {
  my ($self) = @_;
  return [ {
      -logic_name => 'VariationFactory',
      -module     => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::VariationFactory',
      -meadow_type => 'LOCAL',
      -input_ids   => [ {} ],
      -parameters => { es_url => $self->o('es_url') },
      -flow_into  => { '1'       => ['UpdateVariation'] } }, {
      -logic_name  => 'UpdateVariation',
      -module      => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::UpdateVariation',
      #'-meadow_type => 'LSF',
      -hive_capacity => 25,
      -rc_name       => 'default',
      -parameters    => { 'es_url' => $self->o('es_url') } } ];
}

1;
