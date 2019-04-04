#!/bin/env perl
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

package Bio::EnsEMBL::GTI::GeneSearch::ESClient;

use Moose;
with 'MooseX::Log::Log4perl';

use Search::Elasticsearch;
use Carp;

has 'url' => (is => 'ro', isa => 'Str', required => 1);
has 'index' => (is => 'ro', isa => 'Str', required => 1);
has 'bulk' => (is => 'rw', isa => 'Search::Elasticsearch::Client::6_0::Bulk');
has 'timeout' => (is => 'rw', isa => 'Int', default => 3600);

sub BUILD {
    my ($self) = @_;

    $self->log->info("Connecting to " . $self->url());
    my $es =
        Search::Elasticsearch->new(
            # client          => "6_00::Direct",
            nodes           => [ $self->url() ],
            request_timeout => $self->timeout());
    my $bulk = $es->bulk_helper(
        index       => $self->index(),
        timeout     => $self->timeout() . 's',
        on_error    => sub {
            my ($action, $response, $i) = @_;
            $self->handle_error($action, $response, $i);
        },
        on_conflict => sub {
            my ($action, $response, $i) = @_;
            $self->handle_conflict($action, $response, $i);
        });
    $self->{es} = $es;
    $self->bulk($bulk);
    $self->log->info("Connected to " . $self->url());

    return;
}

sub search {
    my ($self) = @_;
    return $self->{es};
}

sub handle_error {
    my ($self, $action, $response, $i) = @_;
    croak "Failed to execute $action $i due to " . $response->{error}->{type} . " error: " . $response->{error}->{reason};
    return;
}

sub handle_conflict {
    my ($self, $action, $response, $i) = @_;
    # no action required
    return;
}

1;
