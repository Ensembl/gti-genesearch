var searchMod = angular.module('search', [ 'datatables' ]);

searchMod.controller('searchController', function(DTOptionsBuilder,
		DTColumnBuilder) {

	var vm = this;
	vm.dtInstance = {};
	vm.hasData = false;
	vm.reloadData = function() {
		vm.dtInstance.reloadData();
	};

	this.search = function(search) {
		
		if(!search) {
			search = {};
		}

		if(!search.query) {
			search.query="{}";
		}
		if(!search.fields) {
			search.fields='["id","genome","description","biotype"]';
		}

		
		alert(JSON.stringify(search));

		if (vm.hasData) {
			vm.reloadData();
		}

		vm.dtOptions = DTOptionsBuilder.newOptions().withOption('ajax', {
			url : 'http://localhost:8080/api/query',
			type : 'POST',
			contentType : 'application/json',
			data : function(data) {
							
				return JSON.stringify({
					"query" : JSON.parse(search.query),
					"fields" : JSON.parse(search.fields)
				});
			},
			dataFilter : function(json) {
				alert(json);
				response = JSON.parse(json);
				response.draw = 1;
				response.recordsTotal = response.resultCount;
				response.recordsFiltered = response.resultCount;
				return JSON.stringify(response);
			}
		}).withDataProp('results').withOption('serverSide', true)
				.withPaginationType('full_numbers');

		vm.dtColumns = [];
		JSON.parse(search.fields).forEach(function(col) {
			vm.dtColumns.push(DTColumnBuilder.newColumn(col).withTitle(col));
		});

		vm.hasData = true;

	};

});
