package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFiles_conf;

use strict;
use warnings;

use base ('Bio::EnsEMBL::Hive::PipeConfig::HiveGeneric_conf');

sub resource_classes {
	my ($self) = @_;
	return { 'default' => { 'LSF' => '-q production-rh6' } };
}

sub default_options {
	my ($self) = @_;
	return { %{ $self->SUPER::default_options() },
			 dumps_dir => '.',
			 es_url    => 'http://127.0.0.1:9200/' };
}

sub pipeline_analyses {
	my ($self) = @_;
	return [ {
		   -logic_name => 'find_files',
		   -module =>
			 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::JsonFileFactory',
		   -meadow_type => 'LOCAL',
		   -input_ids   => [ { dumps_dir => $self->o('dumps_dir') } ],
		   -flow_into => { '1' => ['index_json'] } }, {
		   -logic_name => 'index_json',
		   -module => 'Bio::EnsEMBL::GTI::GeneSearch::Pipeline::IndexJsonFile',
		   -meadow_type   => 'LSF',
		   -hive_capacity => 10,
		   -rc_name       => 'default',
		   -parameters    => { 'es_url' => $self->o('es_url') } } ];
}

1;
