package org.ensembl.genesearch.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.ensembl.genesearch.utils.VcfUtils.ColumnFormat;
import org.ensembl.genesearch.utils.VcfUtils.VcfFormat;
import org.junit.Test;

import htsjdk.samtools.util.BufferedLineReader;

public class VcfUtilsTest {

    @Test
	public void testSpec() throws IOException {
		BufferedLineReader vcfR = new BufferedLineReader(this.getClass().getResourceAsStream("/spec_sample.vcf"));
		VcfFormat format = VcfFormat.readFormat(vcfR);
		//##INFO=<ID=NS,Number=1,Type=Integer,Description="Number of Samples With Data">
        assertEquals("Checking NS", ColumnFormat.INTEGER, format.getFormat("NS"));
        //##INFO=<ID=DP,Number=1,Type=Integer,Description="Total Depth">
        assertEquals("Checking DP", ColumnFormat.INTEGER, format.getFormat("DP"));
		//##INFO=<ID=AF,Number=A,Type=Float,Description="Allele Frequency">
        assertEquals("Checking AF", ColumnFormat.FLOAT_LIST, format.getFormat("AF"));
        //##INFO=<ID=AA,Number=1,Type=String,Description="Ancestral Allele">
        assertEquals("Checking AA", ColumnFormat.STRING, format.getFormat("AA"));
		//##INFO=<ID=DB,Number=0,Type=Flag,Description="dbSNP membership, build 129">
        assertEquals("Checking DB", ColumnFormat.FLAG, format.getFormat("DB"));
		//##INFO=<ID=H2,Number=0,Type=Flag,Description="HapMap2 membership">
        assertEquals("Checking H2", ColumnFormat.FLAG, format.getFormat("H2"));
        List<Map<String, Object>> variants = vcfR.lines().map(VcfUtils::vcfLineToMap).collect(Collectors.toList());
		assertEquals("Variants found", 5, variants.size());		
		Optional<Map<String, Object>> oSnp = variants.stream().filter(v -> "rs6040355".equals(v.get("id"))).findFirst();
		assertTrue("Variant found", oSnp.isPresent());
		//20	1110696	rs6040355	A	G,T	67	PASS	NS=2;DP=10;AF=0.333,0.667;AA=T;DB	GT:GQ:DP:HQ	1|2:21:6:23,27	2|1:2:0:18,2	2/2:35:4:1,1
		Map<String, Object> snp = oSnp.get();
		assertEquals("ID", "rs6040355", snp.get("id"));
		assertEquals("CHROM", "20", snp.get("seq_region_name"));
		assertEquals("POS", "1110696", snp.get("start"));
		assertEquals("REF", "A", snp.get("ref_allele"));
		assertEquals("ALT", "G,T", snp.get("alt_allele"));
		assertEquals("QUAL", "67", snp.get("quality"));
		assertEquals("FILTER", "PASS", snp.get("filter"));
		assertEquals("NS", "2", snp.get("NS"));
		assertEquals("DP", "10", snp.get("DP"));
		assertEquals("AF", "0.333,0.667", snp.get("AF"));
		assertEquals("AA", "T", snp.get("AA"));
		assertEquals("DB", true, snp.get("DB"));
		assertFalse("No genotypes", snp.containsKey("genotypes"));
	}

    @Test
	public void testSpecGenotype() throws IOException {
		BufferedLineReader vcfR = new BufferedLineReader(this.getClass().getResourceAsStream("/spec_sample.vcf"));
        VcfFormat format = VcfFormat.readFormat(vcfR);
		String[] genotypes = format.getGenotypes();
		assertEquals("Genotypes found", 3, genotypes.length);
		List<Map<String, Object>> variants = vcfR.lines().map(l -> VcfUtils.vcfLineToMap(l, format)).collect(Collectors.toList());
		assertEquals("Variants found", 5, variants.size());		
		Optional<Map<String, Object>> oSnp = variants.stream().filter(v -> "rs6040355".equals(v.get("id"))).findFirst();
		assertTrue("Variant found", oSnp.isPresent());
		Map<String, Object> snp = oSnp.get();
		assertEquals("ID", "rs6040355", snp.get("id"));
		assertEquals("CHROM", "20", snp.get("seq_region_name"));
		assertEquals("POS", "1110696", snp.get("start"));
		assertEquals("REF", "A", snp.get("ref_allele"));
		assertEquals("ALT", "G,T", snp.get("alt_allele"));
		assertEquals("QUAL", "67", snp.get("quality"));
		assertEquals("FILTER", "PASS", snp.get("filter"));
		assertEquals("NS", 2, snp.get("NS"));
		assertEquals("DP", 10, snp.get("DP"));
		assertEquals("AF", 2, ((List)snp.get("AF")).size());
		assertEquals("AA", "T", snp.get("AA"));
		assertEquals("DB", true, snp.get("DB"));
		assertTrue("Genotypes", snp.containsKey("genotypes"));
		List<Map<String,Object>> snpGenotypes = (List<Map<String, Object>>) snp.get("genotypes");
		assertEquals("Genotypes found", 3, snpGenotypes.size());
		Map<String,Object> genotype = snpGenotypes.get(0);
		assertEquals("Genotype attributes", 5, genotype.keySet().size());
		assertEquals("GENOTYPE_ID", "NA00001", genotype.get("GENOTYPE_ID"));
		// 1|2:21:6:23,27
		assertEquals("GT", "1|2", genotype.get("GT"));
		assertEquals("GQ", "21", genotype.get("GQ"));
		assertEquals("DP", "6", genotype.get("DP"));
		assertEquals("HQ", "23,27", genotype.get("HQ"));
	}
	
}
