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

package org.ensembl.genesearch;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.ensembl.genesearch.info.FieldType;

/**
 * Specification of a query to invoke against a particular field. Contains a
 * {@link FieldType} plus multiple values which are usually treated as an OR by
 * implementations.
 * 
 * Additional support is provided for numeric ranges, genomic locations and NOT
 * queries (implementation-dependent).
 * 
 * @author dstaines
 *
 */
public class Query {

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
    private final FieldType type;
    private final boolean not;

    public Query(FieldType type, String fieldName) {
        this(type, fieldName, false, null, null);
    }

    public Query(FieldType type, String fieldName, String... values) {
        this(type, fieldName, false, values, null);
    }

    public Query(FieldType type, String fieldName, Collection<String> valuesC) {
        this(type, fieldName, false, valuesC.toArray(new String[valuesC.size()]), null);
    }

    public Query(FieldType type, String fieldName, Query... subQueries) {
        this(type, fieldName, false, null, subQueries);
    }

    public Query(FieldType type, String fieldName, boolean not) {
        this(type, fieldName, not, null, null);
    }

    public Query(FieldType type, String fieldName, boolean not, String... values) {
        this(type, fieldName, not, values, null);
    }

    public Query(FieldType type, String fieldName, boolean not, Collection<String> valuesC) {
        this(type, fieldName, not, valuesC.toArray(new String[valuesC.size()]), null);
    }

    public Query(FieldType type, String fieldName, boolean not, Query... subQueries) {
        this(type, fieldName, not, null, subQueries);
    }

    public Query(FieldType type, String fieldName, boolean not, String[] values, Query[] subQueries) {
        this.type = type;
        this.not = not;
        this.fieldName = fieldName;
        this.values = values;
        this.subQueries = subQueries;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String[] getValues() {
        return values;
    }

    public FieldType getType() {
        return type;
    }

    public Query[] getSubQueries() {
        return subQueries;
    }

    public boolean isNot() {
        return not;
    }

    @Override
    public String toString() {
        if (type == FieldType.NESTED) {
            return StringUtils.join(Arrays.asList(this.type, this.fieldName, Arrays.asList(this.subQueries)), ":");
        } else {
            return StringUtils.join(Arrays.asList(this.type, this.fieldName, Arrays.asList(this.values)), ":");
        }
    }

    /**
     * Expand a query of the form a.b.c:[] into a:{b:{c:[]}}. This is used to
     * allow clients to pass in a simple path string as a key to a sub-document
     * key rather than the full nested structure.
     * 
     * @param q
     *            query to expand
     * @return expanded query
     */
    public static Query expandQuery(Query q) {
        return expandQuery(q.getFieldName(), q.isNot(), Arrays.asList(q.getValues()));
    }

    /**
     * Return a query for a.b.c:[] as a:{b:{c:[]}}. This is used to allow
     * clients to pass in a simple path string as a key to a sub-document key
     * rather than the full nested structure.
     * 
     * @param field
     *            path to field
     * @param not
     *            if true set query to negate
     * @param values
     * @return expanded query
     */
    public static Query expandQuery(String field, boolean not, Collection<String> values) {
        // turn a.b.c into a:{b:{c:ids}}
        int i = field.indexOf('.');
        if (i != -1) {
            return new Query(FieldType.NESTED, field.substring(0, i), false,
                    expandQuery(field.substring(i + 1), not, values));
        } else {
            return new Query(FieldType.TERM, field, not, values);
        }
    }

}