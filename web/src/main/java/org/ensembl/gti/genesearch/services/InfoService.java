package org.ensembl.gti.genesearch.services;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.gti.genesearch.services.info.DataTypeInfo;
import org.ensembl.gti.genesearch.services.info.DataTypeInfoProvider;
import org.ensembl.gti.genesearch.services.info.FieldInfo;
import org.ensembl.gti.genesearch.services.info.FieldInfo.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/fieldinfo")
public class InfoService {

	final Logger log = LoggerFactory.getLogger(InfoService.class);
	protected final DataTypeInfoProvider provider;

	@Autowired
	public InfoService(DataTypeInfoProvider provider) {
		this.provider = provider;
	}

	@GET
	@Produces("application/json")
	public Collection<DataTypeInfo> getDataTypes() {
		return provider.getAll();
	}

	@Path("names")
	@GET
	@Produces("application/json")
	public Collection<String> getDataTypeNames() {
		return provider.getAllNames();
	}

	@Path("{datatype}")
	@GET
	@Produces("application/json")
	public DataTypeInfo getDataType(@PathParam("datatype") String dataType) {
		return provider.getByName(dataType);
	}

	@Path("{datatype}/fields")
	@GET
	@Produces("application/json")
	public Collection<FieldInfo> getFields(@PathParam("datatype") String dataType, @QueryParam("type") String type) {
		if (!StringUtils.isEmpty(type)) {
			switch (type.toLowerCase()) {
			case "facet":
				return provider.getByName(dataType).getFacetableFields();

			case "sort":
				return provider.getByName(dataType).getSortFields();

			case "display":
				return provider.getByName(dataType).getDisplayFields();

			case "search":
				return provider.getByName(dataType).getSearchFields();

			default:
				return provider.getByName(dataType).getFieldByType(FieldType.valueOf(type.toUpperCase()));

			}
		} else {
			return provider.getByName(dataType).getFieldInfo();
		}
	}

}
