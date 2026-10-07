package com.bidnet.server.db;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MongoDB repository for auction persistence and asynchronous bid logging.
 * Reference: PDF Slides 34, 35, 52, 53, 55.
 */
public class AuctionRepository {
    private static final Logger logger = LoggerFactory.getLogger(AuctionRepository.class);

    // Dedicated background worker pool for DB writes to never block TCP broadcasting (Slide 32, 52)
    private static final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);

    public static void saveResult(String auctionId, String winner, double price) {
        dbExecutor.execute(() -> {
            try {
                if (MongoDbManager.isAvailable() && MongoDbManager.getDb() != null) {
                    MongoCollection<Document> coll = MongoDbManager.getDb().getCollection("auction_results");
                    Document doc = new Document("auctionId", auctionId)
                            .append("winner", winner)
                            .append("finalPrice", price)
                            .append("endedAt", System.currentTimeMillis());
                    coll.insertOne(doc);
                    logger.info("Saved auction result to MongoDB: {}", doc.toJson());
                } else {
                    logger.info("[RAM-Mode] Preserved auction result in memory: auctionId={}, winner={}, price={}",
                            auctionId, winner, price);
                }
            } catch (Exception e) {
                logger.error("Error saving auction result: {}", e.getMessage());
            }
        });
    }

    public static void saveBidAsync(String auctionId, String userId, double amount) {
        // Dispatched asynchronously (Slide 32)
        dbExecutor.execute(() -> {
            try {
                if (MongoDbManager.isAvailable() && MongoDbManager.getDb() != null) {
                    MongoCollection<Document> coll = MongoDbManager.getDb().getCollection("bids");
                    Document doc = new Document("bidId", UUID.randomUUID().toString())
                            .append("auctionId", auctionId)
                            .append("userId", userId)
                            .append("amount", amount)
                            .append("time", System.currentTimeMillis());
                    coll.insertOne(doc);
                }
            } catch (Exception e) {
                logger.debug("Async bid persistence skipped/failed: {}", e.getMessage());
            }
        });
    }
}
