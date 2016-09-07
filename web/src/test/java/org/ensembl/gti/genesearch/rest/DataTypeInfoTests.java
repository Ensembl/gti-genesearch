package org.ensembl.gti.genesearch.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.ensembl.genesearch.test.ESTestServer;
import org.ensembl.gti.genesearch.services.info.DataTypeInfo;
import org.ensembl.gti.genesearch.services.info.DataTypeInfoProvider;
import org.ensembl.gti.genesearch.services.info.FieldInfo;
import org.ensembl.gti.genesearch.services.info.JsonDataTypeInfoProvider;
import org.ensembl.gti.genesearch.services.info.FieldInfo.FieldType;
import org.junit.Test;

public class DataTypeInfoTests {

	@Test
	public void testCreation() throws IOException {
		String json = ESTestServer.readResource("/test_datatype_info.json");
		DataTypeInfoProvider provider = new JsonDataTypeInfoProvider(json);
		assertEquals("All expected", 5, provider.getAll().size());
		DataTypeInfo genes = provider.getByName("genes");
		assertNotNull("genes found",genes);
		assertTrue("Genes have targets",genes.getTargets().size()>0);
		assertTrue("Genes have fields",genes.getFieldInfo().size()>0);
		List<FieldInfo> facetableFields = genes.getFacetableFields();
		assertTrue("Faceted fields", facetableFields.size()>0);
		assertTrue("Faceted fields", facetableFields.stream().allMatch(f->f.isFacet()));
		List<FieldInfo> displayFields = genes.getDisplayFields();
		assertTrue("Display fields", displayFields.size()>0);
		assertTrue("Display fields", displayFields.stream().allMatch(f->f.isDisplay()));
		List<FieldInfo> searchFields = genes.getSearchFields();
		assertTrue("Search fields", searchFields.size()>0);
		assertTrue("Search fields", searchFields.stream().allMatch(f->f.isSearch()));
		assertEquals("ID is called id", "id", genes.getFieldByName("id").getName());
		assertTrue("Strands found", genes.getFieldByType(FieldType.STRAND).size()>0);
	}

}
