package org.ensembl.genesearch;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

public class GeneQuery {

	public enum GeneQueryType {
		TERM, RANGE, NESTED;
	}

	private final String fieldName;
	private final String[] values;
	private final GeneQuery[] subQueries;
	private final GeneQuery.GeneQueryType type;
	private final Long start;
	private final Long end;

	public GeneQuery(GeneQuery.GeneQueryType type, String fieldName, Long start, Long end) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = null;
		this.subQueries = null;
		this.start = start;
		this.end = end;
	}

	public GeneQuery(GeneQuery.GeneQueryType type, String fieldName, String... values) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = values;
		this.subQueries = null;
		this.start = null;
		this.end = null;
	}

	public GeneQuery(GeneQuery.GeneQueryType type, String fieldName, Collection<String> valuesC) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = valuesC.toArray(new String[valuesC.size()]);
		this.subQueries = null;
		this.start = null;
		this.end = null;
	}

	public GeneQuery(GeneQuery.GeneQueryType type, String fieldName, GeneQuery... subQueries) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = null;
		this.subQueries = subQueries;
		this.start = null;
		this.end = null;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String[] getValues() {
		return values;
	}

	public GeneQuery.GeneQueryType getType() {
		return type;
	}

	public GeneQuery[] getSubQueries() {
		return subQueries;
	}

	public Long getStart() {
		return start;
	}

	public Long getEnd() {
		return end;
	}

	public String toString() {
		if (type == GeneQueryType.NESTED) {
			return StringUtils.join(Arrays.asList(this.type, this.fieldName, Arrays.asList(this.subQueries)), ":");
		} else if (type == GeneQueryType.RANGE) {
			if (start != null) {
				if (end == null) {
					return ">=" + start;
				} else {
					return start + "-" + end;
				}
			} else if (end != null) {
				return "<=" + end;
			} else {
				return "-";
			}
		} else {
			return StringUtils.join(Arrays.asList(this.type, this.fieldName, Arrays.asList(this.values)), ":");
		}
	}

}