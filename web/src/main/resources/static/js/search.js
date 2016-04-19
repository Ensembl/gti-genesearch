var searchMod = angular.module('search', [ 'datatables' ]);

var searchCtrl = function($http, DTOptionsBuilder, DTColumnBuilder) {

	var vm = this;
	vm.dtInstance = {};
	vm.hasData = false;
	vm.firstRun = true;
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

		if (vm.hasData) {
			vm.reloadData();
		}

		vm.dtOptions = DTOptionsBuilder.newOptions().withOption('ajax', {
			url : 'http://localhost:8080/api/query',
			type : 'POST',
			contentType : 'application/json',
			data : function(data) {
				var sorts = [];
				// only sort after the
				// first query
				if (!vm.firstRun) {
					for (var i = 0; i < data.order.length; i++) {
						var field = search.fields[data.order[i].column];
						var sort = field.name;
						if (data.order[i].dir == 'desc') {
							sort = '-' + sort;
						}
						sorts.push(sort);
					}
				}
				vm.firstRun = false;
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
				return JSON.stringify(response);
			}
		}).withDataProp('results').withOption('serverSide', true).withOption(
				'bFilter', false).withPaginationType('full_numbers');

		vm.dtColumns = [];
		search.fields.forEach(function(col) {
			var c = DTColumnBuilder.newColumn(col.name).withTitle(
					col.displayName);
			if (!col.search) {
				c.notSortable();
			}
			vm.dtColumns.push(c);
		});

		vm.hasData = true;

	};

};

searchMod.controller('searchController', [ '$http', 'DTOptionsBuilder',
                                   		'DTColumnBuilder', searchCtrl ]);

function map(objs, callback) {
	var results = [];
	objs.forEach(function(obj) {
		results.push(callback(obj));
	});
	return results;
}
