package com.framework.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link IDatabaseClient} implementation for MongoDB. MongoDB is not JDBC-based, so it
 * cannot share {@link JdbcDatabaseClient}'s executor, but it implements the same common
 * interface so callers (and {@link com.arcon.utils.db.DBUtils}) don't need to special-case it.
 *
 * Query/update strings use the format: "collectionName|{json filter or document}"
 * e.g. executeQuery("users|{\"status\":\"active\"}")
 *      executeUpdate("users|{\"status\":\"inactive\"}")   -> inserts a document
 */
public class MongoDatabaseClient implements IDatabaseClient {

    private static final Logger log = LoggerFactory.getLogger(MongoDatabaseClient.class);

    private MongoClient mongoClient;
    private MongoDatabase database;

    @Override
    public void connect(DBConfig config) {
        String uri = buildConnectionUri(config);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .build();
        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(config.getDbName());
        log.info("Connected to MongoDB database '{}'", config.getDbName());
    }

    private String buildConnectionUri(DBConfig config) {
        if (config.getConnectionString() != null && !config.getConnectionString().isBlank()) {
            return config.getConnectionString();
        }
        if (config.getUsername() != null && !config.getUsername().isBlank()) {
            return String.format("mongodb://%s:%s@%s:%d/%s", config.getUsername(), config.getPassword(),
                    config.getHost(), config.getPort(), config.getDbName());
        }
        return String.format("mongodb://%s:%d/%s", config.getHost(), config.getPort(), config.getDbName());
    }

    @Override
    public List<Map<String, Object>> executeQuery(String query) {
        ensureConnected();
        String[] parsed = parse(query);
        MongoCollection<Document> collection = database.getCollection(parsed[0]);
        Bson filter = parsed[1].isBlank() ? new Document() : Document.parse(parsed[1]);

        List<Map<String, Object>> results = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find(filter).iterator()) {
            while (cursor.hasNext()) {
                results.add(cursor.next());
            }
        }
        return results;
    }

    @Override
    public int executeUpdate(String query) {
        ensureConnected();
        String[] parsed = parse(query);
        MongoCollection<Document> collection = database.getCollection(parsed[0]);
        Document document = Document.parse(parsed[1]);
        collection.insertOne(document);
        return 1;
    }

    /** Updates documents matching filterJson with the values in updateJson (using $set). */
    public long updateMany(String collectionName, String filterJson, String updateJson) {
        ensureConnected();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Bson filter = filterJson == null || filterJson.isBlank() ? new Document() : Document.parse(filterJson);
        UpdateResult result = collection.updateMany(filter, new Document("$set", Document.parse(updateJson)));
        return result.getModifiedCount();
    }

    /** Deletes documents matching filterJson. */
    public long deleteMany(String collectionName, String filterJson) {
        ensureConnected();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Bson filter = filterJson == null || filterJson.isBlank() ? new Document() : Document.parse(filterJson);
        DeleteResult result = collection.deleteMany(filter);
        return result.getDeletedCount();
    }

    private String[] parse(String query) {
        int separatorIndex = query.indexOf('|');
        if (separatorIndex < 0) {
            return new String[] { query.trim(), "" };
        }
        return new String[] { query.substring(0, separatorIndex).trim(), query.substring(separatorIndex + 1).trim() };
    }

    @Override
    public boolean isConnected() {
        return mongoClient != null;
    }

    @Override
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("Mongo client is not connected. Call connect(DBConfig) first.");
        }
    }
}
