var searchMod = angular.module('search', [ 'datatables' ]);

var searchCtrl = function($http, $scope, DTOptionsBuilder, DTColumnBuilder) {

	var vm = this;
	vm.dtInstance = {};
	vm.hasData = false;

	vm.reloadData = function() {
		vm.dtInstance.reloadData();
	};

	this.displayFields = [];
	$http.get('/api/fieldinfo?type=display').then(function(response) {
		vm.displayFields = response.data;
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

		vm.dtColumns = [];
		search.fields.forEach(function(col) {
			var c = DTColumnBuilder.newColumn(col.name).withTitle(
					col.displayName);
			if (!col.search) {
				c.notSortable();
			}
			vm.dtColumns.push(c);
		});

		if (vm.hasData) {
			vm.dtInstance.rerender();
		}
		vm.hasData = true;

		vm.dtOptions = DTOptionsBuilder.newOptions().withOption('ajax', {
			url : '/api/query',
			type : 'POST',
			contentType : 'application/json',
			data : function(data) {
				var sorts = [];
				for (var i = 0; i < data.order.length; i++) {
					var field = search.fields[data.order[i].column];
					var sort = field.name;
					if (data.order[i].dir == 'desc') {
						sort = '-' + sort;
					}
					sorts.push(sort);
				}

				return JSON.stringify({
					"query" : JSON.parse(search.query),
					"fields" : map(search.fields, function(f) {
						return f.name
					}),
					sort : sorts,
					offset : data.start,
					limit : data.length
				});
			},
			dataFilter : function(json) {
				response = JSON.parse(json);
				response.recordsTotal = response.resultCount;
				response.recordsFiltered = response.resultCount;
				for (var i = 0; i < response.results.length; i++) {
					for(var j = 0; j< search.fields.length; j++) {
						if(!response.results[i].hasOwnProperty(search.fields[j]))  {
							response.results[i][search.fields[j].name] = '';
						}
					}
				}
				return JSON.stringify(response);
			}
		}).withDataProp('results').withOption('serverSide', true).withOption(
				'processing', true).withOption('bFilter', false)
				.withPaginationType('full_numbers').withOption('order', []);

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
