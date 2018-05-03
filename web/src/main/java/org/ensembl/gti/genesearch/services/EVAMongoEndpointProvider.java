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
        DataTypeInfo variantType = DataTypeInfo.fromResource("/mongo_variants_datatype_info.json");
        Search mongoVariantSearch = new MongoSearch(getMongoCollection(), variantType);
        reg.registerSearch(SearchType.VARIANTS, mongoVariantSearch);

    }

}
