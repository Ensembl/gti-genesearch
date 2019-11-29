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

package org.ensembl.genesearch.test;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.apache.commons.io.IOUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;


/**
 * Utility to create and load an in-memory Mongo test server using
 * {@link MongoServer}. Note that this is included in the main source folder to allow
 * reuse in downstream projects e.g. REST server.
 *
 * @author dstaines
 */
public class MongoTestServer {

    private final Logger log = LoggerFactory.getLogger(MongoTestServer.class);

    private final MongoCollection<Document> collection;

    private MongoClient mongoClient;
    private MongoServer mongoServer;

    private InetSocketAddress serverAddress;

    public MongoTestServer() {
        log.info("Starting Fongo server");

        mongoServer = new MongoServer(new MemoryBackend());
        serverAddress = mongoServer.bind();
        // MongoDatabase database = fongo.getDatabase(MongoTestServer.class.getSimpleName());
        mongoClient = new MongoClient(new ServerAddress(serverAddress));
        String collectionName = MongoTestServer.class.getSimpleName() + "_col";
        collection = mongoClient.getDatabase("testMongo").getCollection(collectionName);
        log.info("Retrieved Fongo collection " + collectionName);
    }


    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    /**
     * Read JSON from the specified Java resource and index
     *
     * @param resourceName
     */
    public void indexDataResource(String resourceName) {
        try {
            indexData(IOUtils.toString(MongoTestServer.class.getResourceAsStream(resourceName), Charset.defaultCharset()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Index a JSON document
     *
     * @param json
     */
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
