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

var searchMod = angular.module('search', [ 'datatables' ]);

function ajax(search) {
	return {
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
				console.log("Sorting on "+sort);
				sorts.push(sort);
			}

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
			return JSON.stringify(response);
		}
	}
};

var searchCtrl = function($http, $scope, DTOptionsBuilder, DTColumnBuilder,
		DTInstance) {

	var vm = this;
	vm.dtInstance = {};
	vm.hasData = false;

	// get list of possible fields to use
	this.allFields = [];
	$http.get('/api/fieldinfo').then(function(response) {
		vm.allFields = response.data;
	}, function(response) {
		alert(JSON.stringify(response));
	});

	this.facetFields = [];
	$http.get('/api/fieldinfo?type=facet').then(function(response) {
		vm.facetFields = response.data;
	}, function(response) {
		alert(JSON.stringify(response));
	});

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

	this.setQueryExample = function(exampleName) {
		if (!$scope.search) {
			$scope.search = {};
		}
		$scope.search.query = examples[exampleName];
	}

	this.search = function(search) {

		if (!search) {
			search = {};
		}

		if (!search.query) {
			search.query = "{}";
		}

		if (!search.fields) {
			search.fields = this.displayFields.slice(0, 4);
		}


		console.log("Setting columns");
		vm.dtColumns = [];
		search.fields.forEach(function(col) {
			var c = DTColumnBuilder.newColumn(col.displayField).withTitle(
					col.displayName);
			vm.dtColumns.push(c);
		});
		if (vm.hasData) {
			console.log("Clearing table");
			vm.dtInstance.changeData(ajax(search));
			//vm.dtInstance.rerender();
		} else {
		console.log("Loading table");
		vm.dtOptions = DTOptionsBuilder.newOptions().withOption('ajax',
				ajax(search)).withDataProp('results').withOption('serverSide',
				true).withOption('processing', true).withOption('bFilter',
				false).withOption("defaultContent", "").withPaginationType(
				'full_numbers').withOption('order', []).withOption("saveState",false);
		vm.hasData = true;
		}

	};
};

searchMod.controller('searchController', [ '$http', '$scope',
		'DTOptionsBuilder', 'DTColumnBuilder', searchCtrl ]);

function map(objs, callback) {
	var results = [];
	objs.forEach(function(obj) {
		results.push(callback(obj));
	});
	return results;
}
