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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        assertEquals("NS", "2", snp.get("sample_n"));
        assertEquals("DP", "10", snp.get("depth"));
        assertEquals("AF", "0.333,0.667", snp.get("allele_freq"));
        assertEquals("AA", "T", snp.get("ancestral_allele"));
        assertEquals("DB", true, snp.get("dbsnp"));
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
		assertEquals("NS", 2, snp.get("sample_n"));
		assertEquals("DP", 10, snp.get("depth"));
		assertEquals("AF", 2, ((List)snp.get("allele_freq")).size());
		assertEquals("AA", "T", snp.get("ancestral_allele"));
		assertEquals("DB", true, snp.get("dbsnp"));
		assertTrue("Genotypes", snp.containsKey("genotypes"));
		List<Map<String,Object>> snpGenotypes = (List<Map<String, Object>>) snp.get("genotypes");
		assertEquals("Genotypes found", 3, snpGenotypes.size());
		Map<String,Object> genotype = snpGenotypes.get(0);
		assertEquals("Genotype attributes", 5, genotype.keySet().size());
		assertEquals("id", "NA00001", genotype.get("id"));
		// 1|2:21:6:23,27
		assertEquals("GT", "1|2", genotype.get("genotype"));
		assertEquals("GQ", "21", genotype.get("genotype_q"));
		assertEquals("DP", "6", genotype.get("depth"));
		assertEquals("HQ", "23,27", genotype.get("haplotype_q"));
	}
	
    @Test
    public void testVEP() {
        BufferedLineReader vcfR = new BufferedLineReader(this.getClass().getResourceAsStream("/vep_sample.vcf"));
        VcfFormat format = VcfFormat.readFormat(vcfR);
        List<Map<String, Object>> variants = vcfR.lines().map(l -> VcfUtils.vcfLineToMap(l, format)).collect(Collectors.toList());
        assertEquals("Variants found", 1, variants.size());     
        Optional<Map<String, Object>> oSnp = variants.stream().filter(v -> "rs75377686".equals(v.get("id"))).findFirst();
        assertTrue(List.class.isAssignableFrom(oSnp.get().get("consequences").getClass()));
    }
    
    @Test
    public void testVEPGT() {
        BufferedLineReader vcfR = new BufferedLineReader(this.getClass().getResourceAsStream("/spec_sample_vep.vcf"));
        VcfFormat format = VcfFormat.readFormat(vcfR);
        List<Map<String, Object>> variants = vcfR.lines().map(l -> VcfUtils.vcfLineToMap(l, format)).collect(Collectors.toList());
        assertEquals("Variants found", 5, variants.size());     
        Optional<Map<String, Object>> oSnp = variants.stream().filter(v -> "rs6040355".equals(v.get("id"))).findFirst();
        assertTrue(List.class.isAssignableFrom(oSnp.get().get("consequences").getClass()));
        try {
            System.out.println(new ObjectMapper().writeValueAsString(variants));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
