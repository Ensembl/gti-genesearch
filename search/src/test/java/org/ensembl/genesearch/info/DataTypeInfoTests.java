package org.ensembl.genesearch.info;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.ensembl.genesearch.info.DataTypeInfo;
import org.ensembl.genesearch.info.DataTypeInfoProvider;
import org.ensembl.genesearch.info.FieldInfo;
import org.ensembl.genesearch.info.JsonDataTypeInfoProvider;
import org.ensembl.genesearch.info.FieldInfo.FieldType;
import org.ensembl.genesearch.test.ESTestServer;
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
	
	@Test
	public void testSearch() throws IOException {
		String json = ESTestServer.readResource("/test_datatype_info.json");
		DataTypeInfoProvider provider = new JsonDataTypeInfoProvider(json);
		assertEquals("All expected", 5, provider.getAll().size());
		DataTypeInfo genes = provider.getByName("genes");
		assertNotNull("genes found",genes);
		List<FieldInfo> infoForFieldName = genes.getInfoForFieldName("id");
		assertTrue("Single ID found", infoForFieldName.size()==1);
		infoForFieldName = genes.getInfoForFieldName("*");
		assertTrue("All fields found", infoForFieldName.size()==genes.getFieldInfo().size());
		infoForFieldName = genes.getInfoForFieldName("id*");
		assertTrue("Single ID found", infoForFieldName.size()==1);
		infoForFieldName = genes.getInfoForFieldName("transcripts.id");
		assertTrue("Single ID found", infoForFieldName.size()==1);
	}
	

}
