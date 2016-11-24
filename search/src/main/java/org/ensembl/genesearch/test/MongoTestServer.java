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

package org.ensembl.genesearch.test;

import java.io.IOException;
import java.util.List;

import org.bson.Document;
import org.ensembl.genesearch.utils.DataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fakemongo.Fongo;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Test instance for indexing data
 * 
 * @author dstaines
 *
 */
public class MongoTestServer {

	private final Logger log = LoggerFactory.getLogger(MongoTestServer.class);

	private final MongoCollection<Document> collection;

	public MongoTestServer() {
		log.info("Starting Fongo server");
		Fongo fongo = new Fongo("testMongo");
		MongoDatabase database = fongo.getDatabase(MongoTestServer.class.getSimpleName());
		String collectionName = MongoTestServer.class.getSimpleName() + "_col";
		collection = database.getCollection(collectionName);
		log.info("Retrieved Fongo collection " + collectionName);
	}

	public void indexDataResource(String resourceName) {
		try {
			indexData(DataUtils.readResource(resourceName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void indexData(String json) {
		// insert some test data
		Document testData = Document.parse(json);
		List list = (List) testData.get("docs");
		log.info("Indexing " + list.size() + " test docs");
		for (Object o : list) {
			collection.insertOne((Document) o);
		}
		log.info("Completed indexing " + list.size() + " test docs");
	}

	public MongoCollection<Document> getCollection() {
		return collection;
	}

	public void disconnect() {
		// no disconnect required with Fongo
	}

}
