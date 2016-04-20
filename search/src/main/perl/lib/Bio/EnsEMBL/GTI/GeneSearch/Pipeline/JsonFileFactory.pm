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

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::JsonFileFactory;
use warnings;
use strict;
use base ('Bio::EnsEMBL::Hive::Process');

use File::Find;
use File::Slurp;
use Data::Dumper;

sub run {
	my $self     = shift @_;
	my $base_dir = $self->param_required("dumps_dir");
	my $files    = [];
	my $blacklist = {};
	if(defined $self->param('blacklist')) {
	    $blacklist = { map {chomp; $_=>1} read_file($self->param('blacklist'))};
	}
	find( 
	    sub { 
		if(m/\.json$/) {
		    (my $name = $_) =~ s/([A-Za-z0-9_]+)\.json$/$1/; 
		    push @$files, { file => $File::Find::name } unless $blacklist->{$name};
		}
	    }, 
	    $base_dir );
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
