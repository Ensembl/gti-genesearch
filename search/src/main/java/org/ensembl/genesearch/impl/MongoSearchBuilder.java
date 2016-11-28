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

package org.ensembl.genesearch.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.ensembl.genesearch.Query;

/**
 * Utility to build MongoDB filter {@link Document} instances from {@link Query}
 * instances
 * 
 * @author dstaines
 *
 */
public class MongoSearchBuilder {

	protected static final String ELEM_MATCH = "$elemMatch";
	protected static final String ID_FIELD = "id";
	private static final Pattern LIST_PATTERN = Pattern.compile("(.*)-list$");

	private MongoSearchBuilder() {
	}

	/**
	 * @param queries
	 * @return Mongo filter document
	 */
	public static Document buildQuery(Iterable<Query> queries) {
		Document doc = new Document();
		for (Query q : queries) {
			switch (q.getType()) {
			case NESTED:
				Matcher m = LIST_PATTERN.matcher(q.getFieldName());
				if (m.matches()) {
					// need to merge the existing elems
					String k = m.group(1);
					Document elemMatch = (Document) doc.get(k);
					if (elemMatch == null) {
						elemMatch = new Document(ELEM_MATCH, buildQuery(Arrays.asList(q.getSubQueries())));
						doc.append(k, elemMatch);
					} else {
						Document subElem = (Document) elemMatch.get(ELEM_MATCH);
						for (String subK : buildQuery(Arrays.asList(q.getSubQueries())).keySet()) {
							elemMatch.append(subK, subElem.get(subK));
						}
					}
				} else {
					Document subQ = buildQuery(Arrays.asList(q.getSubQueries()));
					for (String k : subQ.keySet()) {
						String newK = q.getFieldName() + "." + k;
							appendOrAdd(doc, newK, subQ.get(k));
					}
				}
				break;
			case TERM:
				if (q.getValues().length == 1) {
					String val = q.getValues()[0];
					if (StringUtils.isNumeric(val)) {
						doc.append(q.getFieldName(), Double.valueOf(val));
					} else {
						doc.append(q.getFieldName(), val);
					}
				} else {
					doc.append(q.getFieldName(), new Document("$in", Arrays.asList(q.getValues())));
				}
				break;
			default:
				throw new UnsupportedOperationException("No support for type " + q.getType());
			}
		}
		return doc;
	}

	private static void appendOrAdd(Document doc, String key, Object value) {
		if (doc.containsKey(key)) {
			Object o = doc.get(key);
			if (Document.class.isAssignableFrom(o.getClass())) {
				Document valD = (Document)value;
				for(String k: valD.keySet()) {
					appendOrAdd((Document)o, k, valD.get(k));
				}
			} else if (Collection.class.isAssignableFrom(o.getClass())) {
				((Collection) o).add(value);
			} else {
				List<Object> l = new ArrayList<>();
				l.add(o);
				l.add(value);
				doc.remove(key);
				doc.append(key, l);
			}
		} else {
			doc.append(key, value);
		}
	}

}
