package com.bidnet.server.engine;

import com.bidnet.server.network.BroadcastManager;
import com.bidnet.server.network.ClientHandler;
import com.bidnet.server.network.ConnectionManager;
import com.bidnet.server.protocol.Message;
import com.bidnet.server.protocol.MessageType;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry and room coordinator for all auctions.
 * Reference: PDF Slides 21, 26, 49, 55.
 */
public class AuctionManager {
    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);
    private static final ConcurrentHashMap<String, Auction> auctions = new ConcurrentHashMap<>();

    static {
        // Seed default auction items for demonstrations & grading
        Auction defaultAuction = new Auction("A-999", "MacBook Pro M3 Max", 200000.0);
        auctions.put(defaultAuction.getAuctionId(), defaultAuction);

        Auction carAuction = new Auction("A-100", "Tesla Model 3 Performance", 5000000.0);
        auctions.put(carAuction.getAuctionId(), carAuction);
    }

    public static Auction getAuction(String auctionId) {
        if (auctionId == null) return null;
        return auctions.get(auctionId);
    }

    public static void addAuction(Auction auction) {
        auctions.put(auction.getAuctionId(), auction);
        logger.info("New auction registered: {} ({})", auction.getTitle(), auction.getAuctionId());
    }

    public static void joinRoom(String userId, String auctionId) {
        Auction auction = getAuction(auctionId);
        ClientHandler handler = ConnectionManager.getInstance().getHandler(userId);

        if (auction == null) {
            if (handler != null) {
                Message err = new Message(MessageType.ERROR, "SERVER", auctionId, "{\"error\":\"Auction not found\"}");
                handler.sendMessage(err.toJson());
            }
            return;
        }

        // Subscribe to broadcasts
        BroadcastManager.subscribe(auctionId, userId);

        // Send current auction state to the joining user immediately
        if (handler != null) {
            JsonObject state = new JsonObject();
            state.addProperty("status", "JOINED");
            state.addProperty("auctionId", auction.getAuctionId());
            state.addProperty("title", auction.getTitle());
            state.addProperty("currentBid", auction.getCurrentBid());
            state.addProperty("highestBidder", auction.getHighestBidder());
            state.addProperty("isActive", auction.isActive());

            Message welcome = new Message(MessageType.AUCTION_UPDATE, "SERVER", auctionId, state.toString());
            handler.sendMessage(welcome.toJson());
        }
    }

    public static void leaveRoom(String userId, String auctionId) {
        BroadcastManager.unsubscribe(auctionId, userId);
    }

    public static Collection<Auction> getAllAuctions() {
        return auctions.values();
    }
}
