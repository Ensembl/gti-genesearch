package org.ensembl.gti.genesearch.services;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.gti.genesearch.services.info.FieldInfo;
import org.ensembl.gti.genesearch.services.info.FieldInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Path("/fieldinfo")
public class InfoService {

	final Logger log = LoggerFactory.getLogger(GeneService.class);
	protected final FieldInfoProvider provider;

	@Autowired
	public InfoService(FieldInfoProvider provider) {
		this.provider = provider;
	}

	@GET
	@Produces("application/json")
	public Collection<FieldInfo> fields(@QueryParam("type") String type) {
		if (StringUtils.isEmpty(type) || "all".equalsIgnoreCase(type)) {
			return provider.getAll();
		} else if ("facet".equalsIgnoreCase(type)) {
			return provider.getFacetable();
		} else {
			throw new WebApplicationException("Type " + type + " unknown");
		}
	}

}
