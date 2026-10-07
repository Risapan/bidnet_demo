package com.bidnet.server.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MongoDB database connection manager with fault tolerance.
 * If MongoDB is offline, the system safely falls back to pure RAM in-memory state.
 * Reference: PDF Slides 33, 35, 55.
 */
public class MongoDbManager {
    private static final Logger logger = LoggerFactory.getLogger(MongoDbManager.class);
    private static final String URI = "mongodb://localhost:27017";
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static boolean isAvailable = false;

    public static synchronized void connect() {
        try {
            logger.info("Connecting to MongoDB at {}...", URI);
            com.mongodb.ConnectionString connString = new com.mongodb.ConnectionString(URI);
            com.mongodb.MongoClientSettings settings = com.mongodb.MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .applyToClusterSettings(b -> b.serverSelectionTimeout(1500, java.util.concurrent.TimeUnit.MILLISECONDS))
                    .build();

            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("bidnet_db");
            // Ping database to verify connection
            database.runCommand(new org.bson.Document("ping", 1));
            isAvailable = true;
            logger.info("Connected to MongoDB database 'bidnet_db' successfully.");
            System.out.println("Connected to MongoDB.");
        } catch (Exception e) {
            // Fault Tolerance (Slide 35, 55): In-memory engine keeps running even if MongoDB is down
            isAvailable = false;
            logger.warn("MongoDB is offline or unreachable: {}. Operating in FAULT-TOLERANT RAM MODE.", e.getMessage());
            System.out.println("Notice: MongoDB unreachable. Operating in in-memory RAM mode.");
        }
    }

    public static MongoDatabase getDb() {
        return database;
    }

    public static boolean isAvailable() {
        return isAvailable;
    }
}
