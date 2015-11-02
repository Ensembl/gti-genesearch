package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::JsonFileFactory;
use warnings;
use strict;
use base ('Bio::EnsEMBL::Hive::Process');

use File::Find;

sub run {
	my $self     = shift @_;
	my $base_dir = $self->param_required("dumps_dir");
	my $files    = [];
	find( sub { push @$files, { file => $File::Find::name } if m/\.json$/ }, $base_dir );
	$self->param( 'files', $files );
	return;
}

sub write_output {    # nothing to write out, but some dataflow to perform:
	my $self = shift @_;
	my $files = $self->param('files');
	$self->dataflow_output_id( $files, 1 );
	$self->warning( scalar(@$files) . ' index jobs have been created' );

}

1;
