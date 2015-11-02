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
									   url => $self->param_required("es_url") );
	return;
}

sub run {
	my $self = shift @_;
	my $file = $self->param_required('file');
	$self->{indexer}->index_file($file);
	return;
}

1;
