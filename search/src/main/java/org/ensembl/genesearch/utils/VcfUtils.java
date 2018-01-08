package org.ensembl.genesearch.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VcfUtils {

	public final static Map<String,String> INFO_NAMES = new HashMap<>();
	static {
		INFO_NAMES.put("AA", "ancestralAllele");
		INFO_NAMES.put("AC", "alleleCount");
		INFO_NAMES.put("AF", "alleleFreq");
		INFO_NAMES.put("AN", "alleleN");
		INFO_NAMES.put("BQ", "baseQ");
		INFO_NAMES.put("CIGAR", "cigar");
		INFO_NAMES.put("DB", "dbSNP");
		INFO_NAMES.put("DP", "depth");
		INFO_NAMES.put("END", "end");
		INFO_NAMES.put("H2", "hapmap2");
		INFO_NAMES.put("H3", "hapmap3");
		INFO_NAMES.put("MQ", "mappingQ");
		INFO_NAMES.put("MQ0", "zeroMappingQ");
		INFO_NAMES.put("NS", "sampleN");
		INFO_NAMES.put("SB", "strandBias");
		INFO_NAMES.put("SOMATIC", "somatic");
		INFO_NAMES.put("VALIDATED", "validated");
		INFO_NAMES.put("1000G", "1000genomes");
		INFO_NAMES.put("GT", "genotype");
		INFO_NAMES.put("GQ", "genotypeQ");
		INFO_NAMES.put("HQ", "haplotypeQ");
	}
	
	public final static String[] FIXED_FIELDS = { "seq_region_name", "start", "id", "ref_allele", "alt_allele", "quality", "filter" };

	public static String[] getGenotypes(String columnHeader) {
		String[] cols = columnHeader.split("\t");
		if (cols.length > 9) {
			return Arrays.copyOfRange(cols, 9, cols.length);
		} else {
			return new String[] {};
		}
	}

	public static Map<String, Object> vcfLineToMap(String line) {
		return vcfLineToMap(line, null);
	}

	private static final Pattern INFO_PATTERN = Pattern.compile("([^=]+)=(.*)");
	private static final Pattern COLS_PATTERN = Pattern.compile("^#CHROM.+");
	private static final Pattern META_PATTERN = Pattern.compile("^##[^#]+");
	
	public static Predicate<String> isColsLine() {
		return line -> COLS_PATTERN.matcher(line).matches();
	}

	public static Predicate<String> isMetaLine() {
		return line -> META_PATTERN.matcher(line).matches();
	}

	public static Map<String, Object> vcfLineToMap(String line, String[] genotypes) {
		Map<String, Object> map = new HashMap<>();
		String[] cols = line.split("\t");
		for (int n = 0; n < FIXED_FIELDS.length; n++) {
			map.put(FIXED_FIELDS[n], cols[n]);
		}
		for (String infoStr : cols[7].split(";")) {
			Matcher m = INFO_PATTERN.matcher(infoStr);
			if (m.matches()) {
				map.put(m.group(1), m.group(2));
			} else {
				map.put(infoStr, true);
			}
		}
		if (genotypes != null) {
			// get format names
			String[] format = cols[8].split(":");
			// add phenotypes to list
			List<Map<String, Object>> genotypeObjs = new ArrayList<>(genotypes.length);
			for (int n = 9; n < cols.length; n++) {
				Map<String, Object> genotype = new HashMap<>(format.length + 1);
				genotype.put("GENOTYPE_ID", genotypes[n - 9]);
				String[] genotypeVals = cols[n].split(":");
				for (int m = 0; m < format.length; m++) {
					genotype.put(format[m], genotypeVals[m]);
				}
				genotypeObjs.add(genotype);
			}
			map.put("genotypes", genotypeObjs);
		}
		return map;
	}
}
