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

package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::JsonFileFactory;
use warnings;
use strict;
use base ('Bio::EnsEMBL::Production::Pipeline::Common::Base');

use File::Find;
use File::Slurp;
use Data::Dumper;

sub run {
	my $self     = shift @_;
	my $base_dir = $self->param_required("dumps_dir");
	my $genome_files    = [];
	my $genes_files    = [];
	my $variants_files    = [];
	my $blacklist = {};
	if(defined $self->param('blacklist')) {
	    $blacklist = { map {chomp; $_=>1} read_file($self->param('blacklist'))};
	}
	$self->log->info("Processing files from $base_dir");
	find( 
	    sub { 
	      $self->log->debug("Processing ".$_);
		if(m/_genome\.json$/) {
		    (my $name = $_) =~ s/([A-Za-z0-9_]+)_genome\.json$/$1/; 
		    $self->log->debug("genome file for $name");
		    push @$genome_files, { file => $File::Find::name } unless $blacklist->{$name};
		} elsif(m/_genes\.json$/) {
		    (my $name = $_) =~ s/([A-Za-z0-9_]+)_genes\.json$/$1/; 
		    $self->log->debug("genes file for $name");
		    push @$genes_files, { file => $File::Find::name } unless $blacklist->{$name};
		} elsif(m/_variants\.json$/) {
		    (my $name = $_) =~ s/([A-Za-z0-9_]+)_variants\.json$/$1/; 
		    $self->log->debug("variants file for $name");
		    push @$variants_files, { file => $File::Find::name } unless $blacklist->{$name};
		}
	    }, 
	    $base_dir );
	$self->dataflow_output_id( $genome_files, 2 );
	$self->log->info( scalar(@$genome_files) . ' genome index jobs have been created' );
	$self->dataflow_output_id( $genes_files, 3 );
	$self->log->info( scalar(@$genes_files) . ' genes index jobs have been created' );
	$self->dataflow_output_id( $variants_files, 4 );
	$self->log->info( scalar(@$variants_files) . ' variants index jobs have been created' );
	return;
}

1;
