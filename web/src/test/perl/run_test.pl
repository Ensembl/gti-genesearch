#!/bin/env perl
use warnings;
use strict;
use HTTP::Request;
use LWP::UserAgent;
use Data::Dumper;
use Getopt::Long;
use File::Slurp;
use File::Glob;
use Carp;
use Time::HiRes qw(gettimeofday);
use Log::Log4perl qw(:easy);

my $lwp = LWP::UserAgent->new();

my $opts = {};
GetOptions($opts, 'query=s@', 'action:s', 'fields=s@', 'uri:s', 'limit:s', 'verbose');


if($opts->{verbose}) {
    Log::Log4perl->easy_init($DEBUG);
} else {
    Log::Log4perl->easy_init($INFO);
}
my $logger = get_logger();
 
$opts->{uri} ||= 'http://gti-es-0.ebi.ac.uk:8080';
$opts->{action} ||= 'query';

for my $query (@{$opts->{query}}) {
    for my $query_file (glob($query)) {
        $logger->info("Processing $query_file");
        my $json = '{"query":'. read_file($query_file) .'}';
        if($opts->{action} eq 'query') {            
            execute_post("$opts->{uri}/query",$json);
        } else {
            croak "Action ".$opts->{action}." not supported";
        }
    }
}

sub clear_cache {
    
}

sub execute_post {
    my ($uri,$payload) = @_;
    $logger->debug("POSTing to $uri");
    my $req = HTTP::Request->new( 'POST', $uri );
    $req->header( 'Content-Type' => 'application/json' );
    $req->content( $payload );   
    my $start_time = gettimeofday();
    my $response = $lwp->request( $req );
    my $time = gettimeofday() - $start_time;
    if(!$response->is_success()) {
        croak $response->status_line();
    }
    $logger->debug("Took ".$time);
    return;
}

#
#print Dumper($response);
