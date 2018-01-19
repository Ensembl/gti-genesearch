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

import htsjdk.samtools.util.BufferedLineReader;
import wiremock.org.apache.commons.lang3.StringUtils;

public class VcfUtils {

    public static enum ColumnFormat {
        INTEGER, STRING, FLOAT, INTEGER_LIST, STRING_LIST, FLOAT_LIST, FLAG;
    }

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

        public Map<String, Object> csqToMap(String value) {
            Map<String, Object> csq = new HashMap<>();
            for (String csqStr : value.split(",")) {
                int n = 0;
                for (String val : csqStr.split("\\|")) {
                    String col = CSQ_FIELDS[n++];
                    if (!StringUtils.isEmpty(val)) {
                        csq.put(col, val);
                    }
                }
            }
            return csq;
        }

        public Object valueToObj(String column, String value) {

            if ("CSQ".equals(column)) {
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

    public final static Map<String, String> INFO_NAMES = new HashMap<>();
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

    public final static String[] FIXED_FIELDS = { "seq_region_name", "start", "id", "ref_allele", "alt_allele",
            "quality", "filter" };
    public final static String[] CSQ_FIELDS = "Allele|Consequence|IMPACT|SYMBOL|Gene|Feature_type|Feature|BIOTYPE|EXON|INTRON|HGVSc|HGVSp|cDNA_position|CDS_position|Protein_position".split("\\|");

    public static Map<String, Object> vcfLineToMap(String line) {
        return vcfLineToMap(line, new VcfFormat());
    }

    
    private static final Pattern INFO_HEADER_PATTERN = Pattern
            .compile("##INFO=<ID=([^,]+),Number=([^,]+),Type=([^,]+),Description=.*>");
    private static final Pattern INFO_PATTERN = Pattern.compile("([^=]+)=(.*)");
    private static final Pattern COLS_PATTERN = Pattern.compile("^#CHROM.+");
    private static final Pattern META_PATTERN = Pattern.compile("^##[^#]+");

    public static Predicate<String> isInfoHeaderLine() {
        return line -> line.startsWith("##INFO");
    }

    public static Predicate<String> isColsLine() {
        return line -> COLS_PATTERN.matcher(line).matches();
    }

    public static Predicate<String> isMetaLine() {
        return line -> META_PATTERN.matcher(line).matches();
    }

    public static Map<String, Object> vcfLineToMap(String line, VcfFormat format) {
        Map<String, Object> map = new HashMap<>();
        String[] cols = line.split("\t");
        for (int n = 0; n < FIXED_FIELDS.length; n++) {
            map.put(FIXED_FIELDS[n], cols[n]);
        }
        for (String infoStr : cols[7].split(";")) {
            Matcher m = INFO_PATTERN.matcher(infoStr);
            if (m.matches()) {
                map.put(m.group(1), format.valueToObj(m.group(1), m.group(2)));
            } else {
                map.put(infoStr, true);
            }
        }
        if (format.getGenotypes() != null && format.getGenotypes().length>0) {
            // get format names
            String[] gFormat = cols[8].split(":");
            // add phenotypes to list
            List<Map<String, Object>> genotypeObjs = new ArrayList<>(format.getGenotypes().length);
            for (int n = 9; n < cols.length; n++) {
                Map<String, Object> genotype = new HashMap<>(gFormat.length + 1);
                genotype.put("GENOTYPE_ID", format.getGenotypes()[n - 9]);
                String[] genotypeVals = cols[n].split(":");
                for (int m = 0; m < gFormat.length; m++) {
                    genotype.put(gFormat[m], genotypeVals[m]);
                }
                genotypeObjs.add(genotype);
            }
            map.put("genotypes", genotypeObjs);
        }
        return map;
    }
}
