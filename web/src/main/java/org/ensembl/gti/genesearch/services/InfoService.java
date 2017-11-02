package org.ensembl.gti.genesearch.services;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.FieldType;
import org.glassfish.jersey.server.JSONP;

@Produces({ MediaType.APPLICATION_JSON, Application.APPLICATION_X_JAVASCRIPT })
public abstract class InfoService extends SearchBasedService {

    public static final String SEARCH = "search";
    public static final String DISPLAY = "display";
    public static final String SORT = "sort";
    public static final String FACET = "facet";

    public InfoService(EndpointSearchProvider provider) {
		super(provider);
	}

	@Path("info")
	@GET
	@JSONP
	public DataTypeInfo getDataType() {
		return getSearch().getDataType();
	}

	@Path("info/fields")
	@GET
	@JSONP
	public Collection<FieldInfo> getFields(@QueryParam("type") String type) {
		DataTypeInfo dataType = getDataType();
		if (!StringUtils.isEmpty(type)) {
			switch (type.toLowerCase()) {
			case FACET:
				return dataType.getFacetableFields();

			case SORT:
				return dataType.getSortFields();

			case DISPLAY:
				return dataType.getDisplayFields();

			case SEARCH:
				return dataType.getSearchFields();

			default:
				return dataType.getFieldByType(FieldType.valueOf(type.toUpperCase()));

			}
		} else {
			return dataType.getFieldInfo();
		}
	}

}
