/*
 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var examples = {
	human : '{"genome":"homo_sapiens"}',
	e_coli : '{"genome":"escherichia_coli_str_k_12_substr_mg1655"}',
	eschericia_lacZ : '{"lineage":"561","name":"lacZ"}',
	escherichia_signals : '{"lineage":"561","GO":"GO:0035556"}',
	wheat_hypervirulence : '{"annotations":{"host":"4565","phenotype":"PHI:1000008"}}',
	ascomycota_hydrolase : '{"lineage":"4890","GO":"GO:0016787"}',
	mammal_brca2_homologues : '{"lineage":"40674","Pfam":"PF09121","homologues":{"stable_id":"ENSG00000139618"}}',
	human_chr1 : '{"genome":"homo_sapiens","location":{"seq_region_name":"1","start":"45000","end":"96000"}}',
	uniprots : '{"Uniprot_SWISSPROT":["P03886","P03891","P00395","P00403"]}'
};

var setQueryExample = function(exampleName) {
	console.log("Setting example "+exampleName)
	$('#query').val(examples[exampleName]);
};

var allFields;
$(document).ready(function() {
	$('#search').hide();
	$.get("/api/fieldinfo", function(data) {
		allFields = data;
		console.log("Fields loaded");
		$('#search').show();
	});
});

var table;
$('#searchButton').click(function() {
	var search = {
		fields : [ {
			"name" : "id",
			"displayName" : "Gene ID",
			"searchField" : "id",
			"displayField" : "id",
			"type" : "TEXT",
			"facet" : false
		}, {
			"name" : "name",
			"displayName" : "Gene name",
			"searchField" : "name",
			"displayField" : "name",
			"type" : "TEXT",
			"facet" : false
		}, {
			"name" : "genome",
			"displayName" : "Genome",
			"searchField" : "genome",
			"displayField" : "genome_display",
			"type" : "TEXT",
			"facet" : true
		} ],
		query : $('#query').val()
	};
	console.trace(search.query);
	var n = 0;
	var columns = [];
	search.fields.forEach(function(column) {
		console.log("Creating field " + column.name);
		columns.push({
			data : column.displayField,
			title : column.displayName,
			type : 'string',
			visible : true,
			sortable : true,
			targets : n++
		});
	});

	var options = {
		processing : true,
		serverSide : true,
		pagingType: 'simple',
		ajax : {
			url : '/api/query',
			type : 'POST',
			contentType : 'application/json',
			data : function(data) {
				console.log("Posting data");
				// setting sort
				var sorts = [];
				for (var i = 0; i < data.order.length; i++) {
					var field = search.fields[data.order[i].column];
					var sort = field.searchField;
					if (data.order[i].dir == 'desc') {
						sort = '-' + sort;
					}
					console.log("Sorting on " + sort);
					sorts.push(sort);
				}
				console.trace(JSON.stringify(data));
				// create post
				return JSON.stringify({
					"query" : JSON.parse(search.query),
					"fields" : map(search.fields, function(f) {
						return f.displayField
					}),
					sort : sorts,
					offset : data.start,
					limit : data.length
				});
			},
			dataFilter : function(json) {
				console.log("Filtering data");
				response = JSON.parse(json);
				// modify output
				response.recordsTotal = response.resultCount;
				response.recordsFiltered = response.resultCount;
				response.data = response.results;
				response.results = undefined;
				console.log("Completed filtering data");
				console.trace(response);
				return JSON.stringify(response);
			}
		},
		columnDefs : columns
	};
	console.trace(options);
	if (table) {
		console.log("Destroying table");
		table.destroy();
	}
	table = $('#results').DataTable(options);
	console.log("Created table");

});

function map(objs, callback) {
	var results = [];
	objs.forEach(function(obj) {
		results.push(callback(obj));
	});
	return results;
}
