#!/bin/env perl

use warnings;
use strict;
use File::Slurp;
use JSON;

my $es = from_json(read_file("/home/dstaines/Desktop/gene_mapping.json"));
my $ns = "org.ensembl.genesearch.avro";
my $types = {Gene=>{
    namespace=> $ns,
    type=> "record",
    name=> "Gene",
    fields=> []
             }};

convert_properties($es->{gene}->{properties}, $types->{Gene}->{fields});

my $x = [];
for my $type (qw/Source Annotation Homologue Linkage_type Phenotype Host Experimental_evidence Associated_xref Xref Protein_feature Translation Transcript Gene/) {
    push $x,$types->{$type};
}
open my $out, ">", "gene.avsc";
print $out to_json($x);
close $out;


sub convert_properties {
    my ($props, $fields) = @_;
    while (my ($k,$v) = each %{$props}) {
        $v->{name} = $k;
        $v->{name} =~ s/[^A-Za-z0-9]+/_/g;
        delete $v->{index};
        if($v->{type} eq 'integer') {
            $v->{type} = 'int';
        } elsif($v->{type} eq 'short') {
            $v->{type} = 'boolean';
        } elsif($v->{type} eq 'nested') {
            my $type = ucfirst($v->{name});
            $type =~ s/s$//;
            if(!defined $types->{$type}) {
                my $t = {type=>"record", namespace=>$ns, name=>$type, fields=>[]};
                convert_properties($v->{properties},$t->{fields});
                $types->{$type} = $t;
            }
            $v->{type} = {type=>'array',items=>$type};            
            delete $v->{properties};
        } 
        push @{$fields}, $v;
    }
    return;
}
