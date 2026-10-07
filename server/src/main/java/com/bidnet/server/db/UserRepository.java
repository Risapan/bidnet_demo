package com.bidnet.server.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for user management, credential lookups, and BCrypt salted hashing.
 * Reference: PDF Slides 35, 41.
 */
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    // RAM fallback store for users
    private static final ConcurrentHashMap<String, String> inMemoryUsers = new ConcurrentHashMap<>();

    static {
        // Seed admin: password "1234" hashed via BCrypt
        String hashedAdminPw = BCrypt.hashpw("1234", BCrypt.gensalt());
        inMemoryUsers.put("admin", hashedAdminPw);

        // Seed demo bidder U1: password "pass1"
        String hashedU1 = BCrypt.hashpw("pass1", BCrypt.gensalt());
        inMemoryUsers.put("U1", hashedU1);
    }

    public static String getPasswordHash(String username) {
        try {
            if (MongoDbManager.isAvailable() && MongoDbManager.getDb() != null) {
                MongoCollection<Document> coll = MongoDbManager.getDb().getCollection("users");
                Document userDoc = coll.find(Filters.eq("username", username)).first();
                if (userDoc != null) {
                    return userDoc.getString("passHash");
                }
            }
        } catch (Exception e) {
            logger.debug("Mongo query skipped, falling back to RAM cache: {}", e.getMessage());
        }
        return inMemoryUsers.get(username);
    }

    public static void registerUser(String username, String plainPassword, boolean isAdmin) {
        String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        inMemoryUsers.put(username, hashed);

        try {
            if (MongoDbManager.isAvailable() && MongoDbManager.getDb() != null) {
                MongoCollection<Document> coll = MongoDbManager.getDb().getCollection("users");
                Document doc = new Document("userId", "USR-" + System.currentTimeMillis())
                        .append("username", username)
                        .append("passHash", hashed)
                        .append("isAdmin", isAdmin);
                coll.insertOne(doc);
                logger.info("Registered user '{}' into MongoDB.", username);
            }
        } catch (Exception e) {
            logger.debug("MongoDB user insertion deferred: {}", e.getMessage());
        }
    }
}
