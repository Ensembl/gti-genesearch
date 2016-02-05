package org.ensembl.genesearch;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface GeneSearch {
	
	public class GeneQuery {
		
		public enum GeneQueryType {
			TERM, RANGE, NESTED;
		}
		
		private final String fieldName;
		private final String[] values;
		private final GeneQueryType type;
		
		public GeneQuery(GeneQueryType type, String fieldName, String[] values) {
			this.type = type;
			this.fieldName = fieldName;
			this.values= values;
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
		
	}
	
	public List<Map<String,Object>> query(Collection<GeneQuery> queries, String... fieldNames);
	
	public void query(Consumer<Map<String,Object>> consumer, Collection<GeneQuery> queries,
			String... fieldNames);
	
}
