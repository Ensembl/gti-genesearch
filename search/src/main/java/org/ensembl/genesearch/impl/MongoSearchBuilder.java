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

import java.util.Arrays;
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

	private static final String ID_FIELD = "id";
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
					doc.append(m.group(1), new Document("$elemMatch", buildQuery(Arrays.asList(q.getSubQueries()))));
				} else {
					Document subQ = buildQuery(Arrays.asList(q.getSubQueries()));
					for (String k : subQ.keySet()) {
						doc.append(q.getFieldName() + "." + k, subQ.get(k));
					}
				}
				break;
			case TERM:
				if (q.getValues().length == 1) {
					String val = q.getValues()[0];
					if(StringUtils.isNumeric(val)) {
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
		// collapse single paths
		return doc;
	}

}
