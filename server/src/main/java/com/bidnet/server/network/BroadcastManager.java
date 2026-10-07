package com.bidnet.server.network;

import com.bidnet.server.engine.Auction;
import com.bidnet.server.protocol.Message;
import com.bidnet.server.protocol.MessageType;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pushing state updates to subscribed clients via Pub/Sub pattern.
 * Reference: PDF Slides 28, 29.
 */
public class BroadcastManager {
    private static final Logger logger = LoggerFactory.getLogger(BroadcastManager.class);

    // Auction ID -> Set of User IDs
    private static final ConcurrentHashMap<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    public static void subscribe(String auctionId, String userId) {
        subscriptions.putIfAbsent(auctionId, ConcurrentHashMap.newKeySet());
        subscriptions.get(auctionId).add(userId);
        logger.info("User {} subscribed to auction {}", userId, auctionId);
    }

    public static void unsubscribe(String auctionId, String userId) {
        Set<String> users = subscriptions.get(auctionId);
        if (users != null) {
            users.remove(userId);
            logger.info("User {} unsubscribed from auction {}", userId, auctionId);
        }
    }

    public static void broadcastUpdate(Auction auction) {
        Set<String> users = subscriptions.get(auction.getAuctionId());
        if (users == null || users.isEmpty()) return;

        String json = createUpdateJson(auction);
        for (String userId : users) {
            ClientHandler handler = ConnectionManager.getInstance().getHandler(userId);
            if (handler != null) {
                handler.sendMessage(json);
            }
        }
    }

    public static void broadcastEnd(Auction auction) {
        Set<String> users = subscriptions.get(auction.getAuctionId());
        if (users == null || users.isEmpty()) return;

        JsonObject payload = new JsonObject();
        payload.addProperty("status", "AUCTION_ENDED");
        payload.addProperty("finalPrice", auction.getCurrentBid());
        payload.addProperty("winner", auction.getHighestBidder());
        payload.addProperty("title", auction.getTitle());

        Message endMsg = new Message(MessageType.AUCTION_UPDATE, "SERVER", auction.getAuctionId(), payload.toString());
        String json = endMsg.toJson();

        for (String userId : users) {
            ClientHandler handler = ConnectionManager.getInstance().getHandler(userId);
            if (handler != null) {
                handler.sendMessage(json);
            }
        }
    }

    public static String createUpdateJson(Auction auction) {
        JsonObject payload = new JsonObject();
        payload.addProperty("currentBid", auction.getCurrentBid());
        payload.addProperty("highestBidder", auction.getHighestBidder());
        payload.addProperty("isActive", auction.isActive());
        payload.addProperty("title", auction.getTitle());

        Message msg = new Message(MessageType.AUCTION_UPDATE, "SERVER", auction.getAuctionId(), payload.toString());
        return msg.toJson();
    }
}
