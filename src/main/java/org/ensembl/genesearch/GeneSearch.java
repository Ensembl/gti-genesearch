package org.ensembl.genesearch;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

public interface GeneSearch {

	public class GeneQuery {

		public enum GeneQueryType {
			TERM, RANGE, NESTED;
		}

		private final String fieldName;
		private final String[] values;
		private final GeneQuery[] subQueries;
		private final GeneQueryType type;

		public GeneQuery(GeneQueryType type, String fieldName, String... values) {
			this.type = type;
			this.fieldName = fieldName;
			this.values = values;
			this.subQueries = null;
		}

		public GeneQuery(GeneQueryType type, String fieldName,
				Collection<String> valuesC) {
			this.type = type;
			this.fieldName = fieldName;
			this.values = valuesC.toArray(new String[valuesC.size()]);
			this.subQueries = null;
		}

		public GeneQuery(GeneQueryType type, String fieldName,
				GeneQuery... subQueries) {
			this.type = type;
			this.fieldName = fieldName;
			this.values = null;
			this.subQueries = subQueries;
		}

		public String getFieldName() {
			return fieldName;
		}

		public String[] getValues() {
			return values;
		}

		public GeneQueryType getType() {
			return type;
		}

		public GeneQuery[] getSubQueries() {
			return subQueries;
		}

		public String toString() {
			if (type == GeneQueryType.NESTED) {
				return StringUtils.join(
						Arrays.asList(this.type, this.fieldName,
								Arrays.asList(this.subQueries)), ":");
			} else {
				return StringUtils.join(
						Arrays.asList(this.type, this.fieldName,
								Arrays.asList(this.values)), ":");
			}
		}

	}

	public List<Map<String, Object>> query(Collection<GeneQuery> queries,
			String... fieldNames);

	public void query(Consumer<Map<String, Object>> consumer,
			Collection<GeneQuery> queries, String... fieldNames);

}
