package Bio::EnsEMBL::GTI::GeneSearch::Pipeline::VariationFactory;
use warnings;
use strict;
use base ('Bio::EnsEMBL::Hive::Process');
use List::MoreUtils qw/natatime/;
use Log::Log4perl qw/:easy/;
Log::Log4perl->easy_init($INFO);

sub log {
  my ($self) = @_;
  if ( !defined $self->{logger} ) {
    $self->{logger} = get_logger();
  }
  return $self->{logger};
}

sub run {
  my $self = shift @_;
  # get variation dbas
  my $variation_dbas =
    Bio::EnsEMBL::Registry->get_all_DBAdaptors( -GROUP => 'variation' );
  $self->{jobs} = [];
  $self->log()->info("Finding databases with variation");
  for my $variation_dba (@$variation_dbas) {
    my $species = $variation_dba->species();
    $self->log()->info("Retrieving gene IDs for $species");
    my $core_dba = Bio::EnsEMBL::Registry->get_DBAdaptor( $species, "core" );
    my $gene_ids =
      $core_dba->dbc()->sql_helper()
      ->execute_simple( -SQL => q/select stable_id from gene/ );
    $self->log()->info( "Found " . scalar(@$gene_ids) . " gene IDs for $species" );
    my $it = natatime( 1024, @$gene_ids );
    while ( my @ids = $it->() ) {
      push @{ $self->{jobs} }, { species => $species, gene_ids => \@ids };
    }
  }
  return;
}

sub write_output {
  my $self = shift @_;
  $self->log("Writing output");
  $self->dataflow_output_id( $self->{jobs}, 1 );
  $self->warning(
               scalar( @{ $self->{jobs} } ) . ' index jobs have been created' );
  return;
}

1;
