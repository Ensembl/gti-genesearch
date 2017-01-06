/*
 * Copyright [1999-2016] EMBL-European Bioinformatics Institute
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ensembl.genesearch;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.query.DefaultQueryHandler;
import org.ensembl.genesearch.query.QueryHandler;

/**
 * Generic query encapsulating lists of key-value terms
 * 
 * @author dstaines
 *
 */
public class Query {

	private static final QueryHandler handler = new DefaultQueryHandler();

	public static final List<Query> build(String json) {
		return handler.parseQuery(json);
	}

	public enum QueryType {
		TEXT, TERM, NESTED, LOCATION, NUMBER;
	}
	
	public static final String GT = ">";
	public static final String GTE = ">=";
	public static final String LT = "<";
	public static final String LTE = "<=";
	public static final Pattern SINGLE_NUMBER = Pattern.compile("([<>]=?)?(-?[0-9.]+)");
	public static final Pattern RANGE = Pattern.compile("(-?[0-9.]+)-(-?[0-9.]+)");
	public static final Pattern LOCATION = Pattern.compile("([^:]+):([0-9.]+)-([0-9.]+)(:([-1]+))?");

	private final String fieldName;
	private final String[] values;
	private final Query[] subQueries;
	private final Query.QueryType type;

	public Query(Query.QueryType type, String fieldName) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = null;
		this.subQueries = null;
	}

	public Query(Query.QueryType type, String fieldName, String... values) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = values;
		this.subQueries = null;
	}

	public Query(Query.QueryType type, String fieldName, Collection<String> valuesC) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = valuesC.toArray(new String[valuesC.size()]);
		this.subQueries = null;
	}

	public Query(Query.QueryType type, String fieldName, Query... subQueries) {
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

	public Query.QueryType getType() {
		return type;
	}

	public Query[] getSubQueries() {
		return subQueries;
	}

	@Override
	public String toString() {
		if (type == QueryType.NESTED) {
			return StringUtils.join(Arrays.asList(this.type, this.fieldName, Arrays.asList(this.subQueries)), ":");
		} else {
			return StringUtils.join(Arrays.asList(this.type, this.fieldName, Arrays.asList(this.values)), ":");
		}
	}

	/**
	 * Expand a query of the form a.b.c:[] into a:{b:{c:[]}}
	 * 
	 * @param q
	 *            query to expand
	 * @return expanded query
	 */
	public static Query expandQuery(Query q) {
		return expandQuery(q.getFieldName(), Arrays.asList(q.getValues()));
	}

	/**
	 * Return a query for a.b.c:[] as a:{b:{c:[]}}
	 * 
	 * @param field
	 * @param values
	 * @return expanded query
	 */
	public static Query expandQuery(String field, Collection<String> values) {
		// turn a.b.c into a:{b:{c:ids}}
		int i = field.indexOf('.');
		if (i != -1) {
			return new Query(QueryType.NESTED, field.substring(0, i), expandQuery(field.substring(i + 1), values));
		} else {
			return new Query(QueryType.TERM, field, values);
		}
	}

}