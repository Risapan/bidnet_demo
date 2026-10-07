package com.bidnet.server.engine;

import com.bidnet.server.db.AuctionRepository;
import com.bidnet.server.network.BroadcastManager;
import com.bidnet.server.network.ClientHandler;
import com.bidnet.server.network.ConnectionManager;
import com.bidnet.server.protocol.Message;
import com.bidnet.server.protocol.MessageType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-concurrency critical section for bid validation and processing.
 * Reference: PDF Slides 26, 27, 32, 38, 52, 58, 59, 62.
 */
public class BidProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BidProcessor.class);

    public static void process(Message msg) {
        String auctionId = msg.getAuctionId();
        Auction auction = AuctionManager.getAuction(auctionId);

        if (auction == null || !auction.isActive()) {
            sendReject(msg.getSenderId(), auctionId, "AUCTION_CLOSED");
            return;
        }

        // Input validation: reject negative numbers and non-numeric strings (Slide 59)
        double bidAmount;
        try {
            bidAmount = parseAmount(msg.getPayload());
            if (bidAmount <= 0 || Double.isNaN(bidAmount) || Double.isInfinite(bidAmount)) {
                sendReject(msg.getSenderId(), auctionId, "INVALID_BID_AMOUNT");
                return;
            }
        } catch (Exception e) {
            logger.warn("Malformed bid payload from {}: {}", msg.getSenderId(), msg.getPayload());
            sendReject(msg.getSenderId(), auctionId, "MALFORMED_PAYLOAD");
            return;
        }

        // ACQUIRE LOCK - Block other threads bidding on this item (Slide 27, 42, 62)
        auction.getLock().lock();
        try {
            // Enforce minimum increment (Slide 38: 5% minimum increment over current bid)
            double currentBid = auction.getCurrentBid();
            double minRequiredBid = ("None".equals(auction.getHighestBidder())) ? currentBid : currentBid * 1.05;

            if (bidAmount >= minRequiredBid) {
                auction.updateBid(bidAmount, msg.getSenderId());
                logger.info("Bid accepted: Rs. {} by {} on {}", bidAmount, msg.getSenderId(), auctionId);

                // Push update to all subscribed clients
                BroadcastManager.broadcastUpdate(auction);

                // Send acceptance confirmation to bidder
                sendAccept(msg.getSenderId(), auctionId, bidAmount);

                // MongoDB saves occur on a separate worker thread (Slide 32, 52)
                AuctionRepository.saveBidAsync(auctionId, msg.getSenderId(), bidAmount);
            } else {
                logger.info("Bid rejected: Rs. {} is below minimum requirement of Rs. {}", bidAmount, minRequiredBid);
                sendReject(msg.getSenderId(), auctionId, "BID_TOO_LOW");
            }
        } finally {
            auction.getLock().unlock(); // ALWAYS release lock in finally block (Slide 27)
        }
    }

    public static double parseAmount(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty payload");
        }
        JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
        return json.get("amount").getAsDouble();
    }

    private static void sendReject(String userId, String auctionId, String reason) {
        ClientHandler handler = ConnectionManager.getInstance().getHandler(userId);
        if (handler != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("status", "REJECTED");
            payload.addProperty("reason", reason);
            Message msg = new Message(MessageType.BID_REJECTED, "SERVER", auctionId, payload.toString());
            handler.sendMessage(msg.toJson());
        }
    }

    private static void sendAccept(String userId, String auctionId, double amount) {
        ClientHandler handler = ConnectionManager.getInstance().getHandler(userId);
        if (handler != null) {
            JsonObject payload = new JsonObject();
            payload.addProperty("status", "ACCEPTED");
            payload.addProperty("amount", amount);
            Message msg = new Message(MessageType.BID_ACCEPTED, "SERVER", auctionId, payload.toString());
            handler.sendMessage(msg.toJson());
        }
    }
}
