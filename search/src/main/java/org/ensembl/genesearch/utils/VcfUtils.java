/*
 *  See the NOTICE file distributed with this work for additional information
 *  regarding copyright ownership.
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.ensembl.genesearch.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.impl.HtsGetClient;

import htsjdk.samtools.util.BufferedLineReader;

/**
 * Utilities for parsing VCF into Map objects. Used by {@link HtsGetClient} to
 * parse VCF retrieved from htsget API.
 * 
 * These utilities depend on parsing the VCF headers into a {@link VcfFormat}
 * object which can then be used to parse each line
 * 
 * @author dstaines
 *
 */
public class VcfUtils {

	private static final String GENOTYPE_HEADER = "genotype";
	private static final String ZYGOSITY = "heterozygosity";
	private static final String GENOTYPES = "genotypes";
    private static final String CONSEQUENCES = "CSQ";

    /**
     * Column data types supported by this parser
     * 
     * @author dstaines
     *
     */
    public static enum ColumnFormat {
        INTEGER, STRING, FLOAT, INTEGER_LIST, STRING_LIST, FLOAT_LIST, FLAG;
    }

    /**
     * Class representing format as derived from VCF headers. Includes list of
     * genotypes and additional INFO elements with associated types.
     * 
     * @author dstaines
     *
     */
    public static class VcfFormat {
        private String[] genotypes;
        private Map<String, ColumnFormat> formats = new HashMap<>();

        public void setGenotypes(String columnHeader) {
            String[] cols = columnHeader.split("\t");
            if (cols.length > 9) {
                genotypes = Arrays.copyOfRange(cols, 9, cols.length);
            } else {
                genotypes = new String[] {};
            }
        }

        public String[] getGenotypes() {
            return genotypes;
        }

        public ColumnFormat getFormat(String column) {
            ColumnFormat format = formats.get(column);
            if (format == null) {
                format = ColumnFormat.STRING;
            }
            return format;
        }

        public void addFormat(String line) {
            Matcher m = INFO_HEADER_PATTERN.matcher(line);
            if (!m.matches()) {
                throw new IllegalArgumentException("Not an INFO line: " + line);
            }
            boolean isSingle = m.group(2).equals("1");
            ColumnFormat format = isSingle ? ColumnFormat.STRING : ColumnFormat.STRING_LIST;
            switch (m.group(3)) {
            case "Integer":
                format = isSingle ? ColumnFormat.INTEGER : ColumnFormat.INTEGER_LIST;
                break;
            case "Float":
                format = isSingle ? ColumnFormat.FLOAT : ColumnFormat.FLOAT_LIST;
                break;
            case "Flag":
                format = ColumnFormat.FLAG;
            }
            formats.put(m.group(1), format);
        }

        public List<Map<String, Object>> csqToMap(String value) {
            List<Map<String, Object>> csqs = new ArrayList<>();
            for (String csqStr : value.split(",")) {
                Map<String, Object> csq = new HashMap<>();
                int n = 0;
                for (String val : csqStr.split("\\|")) {
                    String col = CSQ_FIELDS[n++];
                    if (!StringUtils.isEmpty(val)) {
                    		if(CONSEQUENCE.equals(col)) {
                    			csq.put(col, Arrays.asList(val.split("&")));
                    		} else {
                    			csq.put(col, val);
                    		}
                    }
                }
                csqs.add(csq);
            }
            return csqs;
        }

        /**
         * Parse a given column into an object based on the parsed format
         * 
         * @param column
         *            name of column
         * @param value
         *            value of column
         * @return
         */
        public Object valueToObj(String column, String value) {

            if (CONSEQUENCES.equals(column)) {
                return csqToMap(value);
            }

            switch (getFormat(column)) {
            case INTEGER:
                return Integer.valueOf(value);
            case FLOAT:
                return Float.valueOf(value);
            case STRING_LIST:
                return Arrays.asList(value.split(","));
            case INTEGER_LIST:
                return Arrays.asList(value.split(",")).stream().map(Integer::parseInt).collect(Collectors.toList());
            case FLOAT_LIST:
                return Arrays.asList(value.split(",")).stream().map(Float::parseFloat).collect(Collectors.toList());
            case FLAG:
                return true;
            case STRING:
            default:
                return value;
            }
        }

        /**
         * Entry point to parse the header lines from a VCF reader and generate
         * a format object for subsequent use
         * 
         * @param vcfR
         * @return
         */
        public static VcfFormat readFormat(BufferedLineReader vcfR) {
            VcfFormat format = new VcfFormat();
            String line = null;
            // read until COLS line is found
            // pick up any INFO lines on the way
            while ((line = vcfR.readLine()) != null) {
                if (isInfoHeaderLine().test(line)) {
                    format.addFormat(line);
                } else if (isColsLine().test(line)) {
                    format.setGenotypes(line);
                    break;
                }
            }
            return format;
        }
    }

    /**
     * Known INFO fields with recased map key names to use in output
     */
    public final static Map<String, String> INFO_NAMES = new HashMap<>();
    static {
        INFO_NAMES.put("AA", "ancestral_allele");
        INFO_NAMES.put("AC", "allele_count");
        INFO_NAMES.put("AF", "allele_freq");
        INFO_NAMES.put("AN", "allele_n");
        INFO_NAMES.put("BQ", "base_q");
        INFO_NAMES.put("CIGAR", "cigar");
        INFO_NAMES.put("DB", "dbsnp");
        INFO_NAMES.put("DP", "depth");
        INFO_NAMES.put("END", "end");
        INFO_NAMES.put("H2", "hapmap2");
        INFO_NAMES.put("H3", "hapmap3");
        INFO_NAMES.put("MQ", "mapping_q");
        INFO_NAMES.put("MQ0", "zero_mapping_q");
        INFO_NAMES.put("NS", "sample_n");
        INFO_NAMES.put("SB", "strand_nias");
        INFO_NAMES.put("SOMATIC", "somatic");
        INFO_NAMES.put("VALIDATED", "validated");
        INFO_NAMES.put("1000G", "1000genomes");
        INFO_NAMES.put("GT", "genotype");
        INFO_NAMES.put("GQ", "genotype_q");
        INFO_NAMES.put("HQ", "haplotype_q");
        INFO_NAMES.put("CSQ", "consequences");
    }

    /**
     * Fields that are always found in VCF
     */
    public final static String[] FIXED_FIELDS = { "seq_region_name", "start", "id", "ref_allele", "alt_allele",
            "quality", "filter" };
    public final static String CONSEQUENCE = "Consequence";

    /**
     * Fields that are found in VEP consequence info types
     */
    public final static String[] CSQ_FIELDS = 
    		"Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|DISTANCE|STRAND|FLAGS|SYMBOL_SOURCE|HGNC_ID|REFSEQ_MATCH|SOURCE|GIVEN_REF|USED_REF|BAM_EDIT|SIFT|PolyPhen".split("\\|");

    /**
     * @param line
     *            VCF line
     * @return Map containing variant data
     */
    public static Map<String, Object> vcfLineToMap(String line) {
        return vcfLineToMap(line, new VcfFormat());
    }

    private static final Pattern INFO_HEADER_PATTERN = Pattern
            .compile("##INFO=<ID=([^,]+),Number=([^,]+),Type=([^,]+),Description=.*>");
    private static final Pattern INFO_PATTERN = Pattern.compile("([^=]+)=(.*)");
    private static final Pattern COLS_PATTERN = Pattern.compile("^#CHROM.+");
    private static final Pattern META_PATTERN = Pattern.compile("^##[^#]+");
    private static final Pattern GENOTYPE_PATTERN = Pattern.compile("([0-9]+)[|/]([0-9]+)");

    /**
     * @return true if line is an INFO header line
     */
    public static Predicate<String> isInfoHeaderLine() {
        return line -> line.startsWith("##INFO");
    }

    /**
     * @return true if this is the COLS line
     */
    public static Predicate<String> isColsLine() {
        return line -> COLS_PATTERN.matcher(line).matches();
    }

    /**
     * @return true if line is a meta line
     */
    public static Predicate<String> isMetaLine() {
        return line -> META_PATTERN.matcher(line).matches();
    }

    /**
     * Convert a VCF line to a map with the pre-parsed format object
     * 
     * @param line
     * @param format
     * @return variant as map
     */
    public static Map<String, Object> vcfLineToMap(String line, VcfFormat format) {
        Map<String, Object> map = new HashMap<>();
        String[] cols = line.split("\t");
        for (int n = 0; n < FIXED_FIELDS.length; n++) {
            map.put(FIXED_FIELDS[n], cols[n]);
        }
        for (String infoStr : cols[7].split(";")) {
            Matcher m = INFO_PATTERN.matcher(infoStr);
            if (m.matches()) {
                map.put(INFO_NAMES.getOrDefault(m.group(1), m.group(1)), format.valueToObj(m.group(1), m.group(2)));
            } else {
                map.put(INFO_NAMES.getOrDefault(infoStr, infoStr), true);
            }
        }
        if (format.getGenotypes() != null && format.getGenotypes().length > 0) {
            // get format names
            List<String> gFormat = Arrays.asList(cols[8].split(":")).stream().map(f -> INFO_NAMES.getOrDefault(f, f))
                    .collect(Collectors.toList());
            // add phenotypes to list
            List<Map<String, Object>> genotypeObjs = new ArrayList<>(format.getGenotypes().length);
            for (int n = 9; n < cols.length; n++) {
                Map<String, Object> genotype = new HashMap<>(gFormat.size() + 1);
                genotype.put("id", format.getGenotypes()[n - 9]);
                String[] genotypeVals = cols[n].split(":");
                for (int m = 0; m < gFormat.size(); m++) {
                    genotype.put(gFormat.get(m), genotypeVals[m]);
                }
                Object gt = genotype.get(GENOTYPE_HEADER);
                if(gt!=null) {
                		Matcher m = GENOTYPE_PATTERN.matcher(String.valueOf(gt));
                		if(m.matches()) {
                			if(m.group(1).equals(m.group(2))) {
                				genotype.put(ZYGOSITY,false);                				
                			} else {
                				genotype.put(ZYGOSITY,true);
                			}
                		}
                }
                genotypeObjs.add(genotype);
            }            
            map.put(GENOTYPES, genotypeObjs);
        }
        return map;
    }
}
