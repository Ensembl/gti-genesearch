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

package org.ensembl.genesearch.impl;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.ensembl.genesearch.Query.GT;
import static org.ensembl.genesearch.Query.GTE;
import static org.ensembl.genesearch.Query.LT;
import static org.ensembl.genesearch.Query.LTE;
import static org.ensembl.genesearch.Query.RANGE;
import static org.ensembl.genesearch.Query.SINGLE_NUMBER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.ensembl.genesearch.Query;

/**
 * Utility methods used by {@link MongoSearch} to build MongoDB filter
 * {@link Document} instances from {@link Query} instances.
 * 
 * Supports TERM, LOCATION, NESTED and NUMBER queries. TEXT fields are treated
 * as TERM. No support for NOT (!) queries
 * 
 * @author dstaines
 *
 */
public class MongoSearchBuilder {

    protected static final String CHR = "Chr";
    public static final String ELEM_MATCH = "$elemMatch";
    protected static final String ID_FIELD = "id";
    private static final Pattern LIST_PATTERN = Pattern.compile("(.*)-list$");
    public static final String SEQ_REGION_FIELD = "chr";
    public static final String START_FIELD = "start";
    public static final String LOCATION_FIELD = "location";

    private MongoSearchBuilder() {
    }

    /**
     * Main method to transform queries to Mongo documents
     * 
     * @param queries
     * @return Mongo filter document
     */
    public static Document buildQuery(Iterable<Query> queries) {
        // base document that all querier are added to
        Document doc = new Document();
        for (Query q : queries) {

            if (q.isNot()) {
                throw new UnsupportedOperationException("No support for NOT queries");
            }

            switch (q.getType()) {
            case LOCATION:
                processLocation(q, doc);
                break;
            case NESTED:
                processNested(q, doc);
                break;
            case NUMBER:
                processNumber(q, doc);
                break;
            case TEXT:
            case TERM:
                processTerm(q, doc);
                break;
            default:
                throw new UnsupportedOperationException("No support for type " + q.getType());
            }
        }
        return doc;
    }

    /**
     * @param q
     *            term query
     * @param doc
     *            document to add query to
     */
    protected static void processTerm(Query q, Document doc) {
        if (q.getValues().length == 1) {
            String val = q.getValues()[0];
            doc.append(q.getFieldName(), val);
        } else {
            doc.append(q.getFieldName(), new Document("$in", Arrays.asList(q.getValues())));
        }
    }

    /**
     * Generate a Mongo query for a location (uses EVA specific fields)
     * 
     * @param q
     * @param doc
     */
    protected static void processLocation(Query q, Document doc) {
        for (String value : q.getValues()) {
            Matcher m = Query.LOCATION.matcher(value);
            if (m.matches()) {
                doc.append(SEQ_REGION_FIELD, CHR + m.group(1));
                doc.append(START_FIELD, new Document().append("$gte", Double.valueOf(m.group(2))).append("$lte",
                        Double.valueOf(m.group(3))));
            } else {
                throw new UnsupportedOperationException("Cannot parse location query " + value);
            }
        }
    }

    /**
     * @param q
     *            nested query
     * @param doc
     *            document to add query to
     */
    protected static void processNested(Query q, Document doc) {
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
    }

    /**
     * @param q
     *            numeric query
     * @param doc
     *            document to add query to
     */
    protected static void processNumber(Query q, Document doc) {
        String value = q.getValues()[0];
        Matcher m = SINGLE_NUMBER.matcher(value);
        if (m.matches()) {
            if (m.groupCount() == 1 || isEmpty(m.group(1))) {
                // EQ
                doc.append(q.getFieldName(), Double.valueOf(value));
            } else {
                String op = m.group(1);
                switch (op) {
                case GT:
                    doc.append(q.getFieldName(), new Document("$gt", Double.valueOf(m.group(2))));
                    break;
                case GTE:
                    doc.append(q.getFieldName(), new Document("$gte", Double.valueOf(m.group(2))));
                    break;
                case LT:
                    doc.append(q.getFieldName(), new Document("$lt", Double.valueOf(m.group(2))));
                    break;
                case LTE:
                    doc.append(q.getFieldName(), new Document("$lte", Double.valueOf(m.group(2))));
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported numeric operator " + op);
                }
            }
        } else {
            m = RANGE.matcher(value);
            if (m.matches()) {
                doc.append(q.getFieldName(), new Document().append("$gte", Double.valueOf(m.group(1))).append("$lte",
                        Double.valueOf(m.group(2))));
            } else {
                throw new UnsupportedOperationException("Cannot parse numeric query " + value);
            }
        }
    }

    /**
     * Utility method to either add a new child to a Mongo document or append to
     * an existing attribute
     * 
     * @param doc
     * @param key
     * @param value
     */
    private static void appendOrAdd(Document doc, String key, Object value) {
        if (doc.containsKey(key)) {
            Object o = doc.get(key);
            if (Document.class.isAssignableFrom(o.getClass())) {
                Document valD = (Document) value;
                for (String k : valD.keySet()) {
                    appendOrAdd((Document) o, k, valD.get(k));
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
