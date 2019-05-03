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
package org.ensembl.gti.genesearch.services;

import org.bson.Document;
import org.ensembl.genesearch.Search;
import org.ensembl.genesearch.SearchType;
import org.ensembl.genesearch.impl.MongoSearch;
import org.ensembl.genesearch.impl.SearchRegistry;
import org.ensembl.genesearch.info.DataTypeInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;

/**
 * Implementation using EVA variation stored in MongoDB
 * 
 * @author dstaines
 *
 */
@Component
@Profile("eva_mongo")
public class EVAMongoEndpointProvider extends EndpointSearchProvider {

    protected MongoCollection<Document> mongoCollection = null;
    @Value("${mongo.url:}")
    private String mongoUrl;
    @Value("${mongo.database:}")
    private String mongoDatabaseName;
    @Value("${mongo.collection:}")
    private String mongoCollectionName;

    public MongoCollection<Document> getMongoCollection() {
        if (mongoCollection == null) {
            log.info("Connecting to MongoDB " + mongoUrl);
            MongoClient mongoC = new MongoClient(new MongoClientURI(mongoUrl));
            log.info("Connecting to MongoDB " + mongoDatabaseName + "/" + mongoCollectionName);
            mongoCollection = mongoC.getDatabase(mongoDatabaseName).getCollection(mongoCollectionName);
        }
        return mongoCollection;
    }

    public void setMongoCollection(MongoCollection<Document> mongoCollection) {
        this.mongoCollection = mongoCollection;
    }

    public EVAMongoEndpointProvider() {
        super();
    }

    @Override
    protected void registerSearches(SearchRegistry reg) {

        super.registerSearches(reg);
        DataTypeInfo variantType = DataTypeInfo.fromResource("/datatypes/mongo_variants_datatype_info.json");
        Search mongoVariantSearch = new MongoSearch(getMongoCollection(), variantType);
        reg.registerSearch(SearchType.VARIANTS, mongoVariantSearch);

    }

}
