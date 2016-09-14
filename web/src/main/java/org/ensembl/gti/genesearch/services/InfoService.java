package org.ensembl.gti.genesearch.services;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.FieldInfo.FieldType;
import org.glassfish.jersey.server.JSONP;

@Produces({ MediaType.APPLICATION_JSON, Application.APPLICATION_X_JAVASCRIPT })
public abstract class InfoService extends SearchBasedService {

	public InfoService(EndpointSearchProvider provider) {
		super(provider);
	}

	@Path("fieldinfo")
	@GET
	@JSONP
	public Collection<DataTypeInfo> getDataTypes() {
		return getSearch().getDataTypes();
	}

	@Path("fieldinfo/names")
	@GET
	@JSONP
	public Collection<String> getDataTypeNames() {
		return getSearch().getDataTypes().stream().map(t -> t.getName()).collect(Collectors.toList());
	}

	@Path("fieldinfo/{datatype}")
	@GET
	@JSONP
	public DataTypeInfo getDataType(@PathParam("datatype") String dataType) {
		return getSearch().getDataTypes().stream().filter(t -> t.getName().equalsIgnoreCase(dataType)).findFirst()
				.orElse(null);
	}

	@Path("fieldinfo/{datatype}/fields")
	@GET
	@JSONP
	public Collection<FieldInfo> getFields(@PathParam("datatype") String dataTypeName,
			@QueryParam("type") String type) {
		DataTypeInfo dataType = getDataType(dataTypeName);
		if (!StringUtils.isEmpty(type)) {
			switch (type.toLowerCase()) {
			case "facet":
				return dataType.getFacetableFields();

			case "sort":
				return dataType.getSortFields();

			case "display":
				return dataType.getDisplayFields();

			case "search":
				return dataType.getSearchFields();

			default:
				return dataType.getFieldByType(FieldType.valueOf(type.toUpperCase()));

			}
		} else {
			return dataType.getFieldInfo();
		}
	}

}
