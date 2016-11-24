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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.bson.Document;
import org.ensembl.genesearch.Query;
import org.ensembl.genesearch.QueryOutput;
import org.ensembl.genesearch.QueryResult;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;

/**
 * Search that uses MongoDB
 * 
 * @author dstaines
 *
 */
public class MongoSearch implements Search {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private static final String MONGO_ID = "_id";
	private final DataTypeInfo dataType;
	private final MongoCollection<Document> mongoC;

	/**
	 * Create a new instance of {@link Search}
	 * 
	 * @param mongoC
	 *            collection to use
	 * @param dataType
	 *            datatype describing supported fields
	 */
	public MongoSearch(MongoCollection<Document> mongoC, DataTypeInfo dataType) {
		this.mongoC = mongoC;
		this.dataType = dataType;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#fetch(java.util.function.Consumer,
	 * java.util.List, org.ensembl.genesearch.QueryOutput)
	 */
	@Override
	public void fetch(Consumer<Map<String, Object>> consumer, List<Query> queries, QueryOutput output) {
		// 1. turn queries into filter
		Document filter = MongoSearchBuilder.buildQuery(queries);
		log.info("Using filter "+filter.toJson());
		// 2. turn output into projection
		if(!output.getFields().contains(getIdField())) {
			output.getFields().add(0, getIdField());
		}
		List<String> fieldNames = output.getFields();
		// 3. execute query and pass to consumer
		mongoC.find(filter).projection(Projections.include(fieldNames))
				.forEach((Document d) -> consumer.accept(documentToMap(d)));
		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#query(java.util.List,
	 * org.ensembl.genesearch.QueryOutput, java.util.List, int, int,
	 * java.util.List)
	 */
	@Override
	public QueryResult query(List<Query> queries, QueryOutput output, List<String> facets, int offset, int limit,
			List<String> sorts) {
		// 1. turn queries into filter
		Document filter = MongoSearchBuilder.buildQuery(queries);
		log.info("Using filter "+filter.toJson());
		// 2. turn output into projection
		if(!output.getFields().contains(getIdField())) {
			output.getFields().add(0, getIdField());
		}
		List<String> fieldNames = output.getFields();
		// 3. execute query
		List<Map<String, Object>> results = new ArrayList<>(offset);
		mongoC.find(filter).limit(limit).skip(offset).projection(Projections.include(fieldNames))
				.forEach((Document d) -> results.add(documentToMap(d)));
		// 4. populate QueryResult
		return new QueryResult(-1L, offset, limit, getFieldInfo(output), results, Collections.emptyMap());
	}

	private static Map<String, Object> documentToMap(Document doc) {
		Map<String, Object> map = new HashMap<>();
		for (Entry<String, Object> e : doc.entrySet()) {
			map.put(e.getKey(), processObject(e.getValue()));
		}
		return map;
	}

	private static Object processObject(Object o) {
		if (Document.class.isAssignableFrom(o.getClass())) {
			return documentToMap((Document) o);
		} else if (Collection.class.isAssignableFrom(o.getClass())) {
			List<Object> os = new ArrayList<>();
			for (Object oo : (Collection) o) {
				os.add(processObject(oo));
			}
			return os;
		} else {
			return o;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#select(java.lang.String, int, int)
	 */
	@Override
	public QueryResult select(String name, int offset, int limit) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.ensembl.genesearch.Search#getDataType()
	 */
	@Override
	public DataTypeInfo getDataType() {
		return dataType;
	}

	@Override
	public String getIdField() {
		return MONGO_ID;
	}
	

}
