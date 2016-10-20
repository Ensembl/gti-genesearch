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
		TEXT, TERM, RANGE, NESTED;
	}

	private final String fieldName;
	private final String[] values;
	private final Query[] subQueries;
	private final Query.QueryType type;
	private final Long start;
	private final Long end;

	public Query(Query.QueryType type, String fieldName, Long start, Long end) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = null;
		this.subQueries = null;
		this.start = start;
		this.end = end;
	}

	public Query(Query.QueryType type, String fieldName, String... values) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = values;
		this.subQueries = null;
		this.start = null;
		this.end = null;
	}

	public Query(Query.QueryType type, String fieldName, Collection<String> valuesC) {
		this.type = type;
		this.fieldName = fieldName;
		this.values = valuesC.toArray(new String[valuesC.size()]);
		this.subQueries = null;
		this.start = null;
		this.end = null;
	}

	public Query(Query.QueryType type, String fieldName, Query... subQueries) {
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

	public Query.QueryType getType() {
		return type;
	}

	public Query[] getSubQueries() {
		return subQueries;
	}

	public Long getStart() {
		return start;
	}

	public Long getEnd() {
		return end;
	}

	public String toString() {
		if (type == QueryType.NESTED) {
			return StringUtils.join(Arrays.asList(this.type, this.fieldName, Arrays.asList(this.subQueries)), ":");
		} else if (type == QueryType.RANGE) {
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