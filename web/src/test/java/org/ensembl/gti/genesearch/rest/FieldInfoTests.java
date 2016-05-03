package org.ensembl.gti.genesearch.rest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.gti.genesearch.services.info.FieldInfo.FieldType;
import org.ensembl.gti.genesearch.services.info.FieldInfoProvider;
import org.ensembl.gti.genesearch.services.info.JsonFieldInfoProvider;
import org.junit.Test;

public class FieldInfoTests {

	@Test
	public void testCreation() throws IOException {
		String json = ESTestServer.readResource("/test_field_info.json");
		FieldInfoProvider provider = new JsonFieldInfoProvider(json);
		assertEquals("All expected", 4, provider.getAll().size());
		assertEquals("Location expected", 1, provider.getByType(FieldType.LOCATION).size());
		assertEquals("Facet expected", 1, provider.getFacetable().size());
		assertEquals("ID", "id", provider.getByName("id").getName());
	}

}
