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

	public static String[] FIXED_FIELDS = { "CHROM", "POS", "ID", "REF", "ALT", "QUAL", "FILTER" };

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
