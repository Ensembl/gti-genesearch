package org.ensembl.genesearch.info;

import org.ensembl.genesearch.utils.DataUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DataTypeInfoTests {

	@Test
	public void testCreation() throws IOException {
		DataTypeInfo genes = DataTypeInfo.fromString(DataUtils.readResource("/datatypes/genes_datatype_info.json"));
		assertNotNull("genes found", genes);
		assertTrue("Genes have targets", genes.getTargets().size() > 0);
		assertTrue("Genes have fields", genes.getFieldInfo().size() > 0);
		List<FieldInfo> facetableFields = genes.getFacetableFields();
		assertTrue("Faceted fields", facetableFields.size() > 0);
		assertTrue("Faceted fields", facetableFields.stream().allMatch(f -> f.isFacet()));
		List<FieldInfo> displayFields = genes.getDisplayFields();
		assertTrue("Display fields", displayFields.size() > 0);
		assertTrue("Display fields", displayFields.stream().allMatch(f -> f.isDisplay()));
		List<FieldInfo> searchFields = genes.getSearchFields();
		assertTrue("Search fields", searchFields.size() > 0);
		assertTrue("Search fields", searchFields.stream().allMatch(f -> f.isSearch()));
		assertEquals("ID is called id", "id", genes.getFieldByName("id").getName());
		assertTrue("Strands found", genes.getFieldByType(FieldType.STRAND).size() > 0);
	}

	@Test
	public void testSearch() throws IOException {
		DataTypeInfo genes = DataTypeInfo.fromResource("/datatypes/genes_datatype_info.json");
		assertNotNull("genes found", genes);
		List<FieldInfo> infoForFieldName = genes.getInfoForFieldName("id");
		assertTrue("Single ID found", infoForFieldName.size() == 1);
		infoForFieldName = genes.getInfoForFieldName("*");
		assertTrue("All fields found", infoForFieldName.size() == genes.getFieldInfo().size());
		infoForFieldName = genes.getInfoForFieldName("id*");
		assertTrue("Single ID found", infoForFieldName.size() == 1);
		infoForFieldName = genes.getInfoForFieldName("transcripts.id");
		assertTrue("Single ID found", infoForFieldName.size() == 1);
	}

	@Test
	public void parseInfo() {
		for (String resourceName : Arrays.asList("/datatypes/genes_datatype_info.json", "/datatypes/genomes_datatype_info.json",
                "/datatypes/transcripts_datatype_info.json", "/datatypes/sequences_datatype_info.json", "/datatypes/homologues_datatype_info.json")) {
			try {
				DataTypeInfo info = DataTypeInfo.fromResource(resourceName);
				assertNotNull("Checking for null "+resourceName,info);
				assertNotNull("Checking for null name "+resourceName,info.getName());
			} catch(RuntimeException e) {
				fail("Could not parse "+resourceName);
			}
		}
	}

}
