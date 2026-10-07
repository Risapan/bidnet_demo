package com.bidnet.server.protocol;

import com.bidnet.server.engine.Auction;
import com.bidnet.server.engine.AuctionManager;
import com.bidnet.server.engine.AuthService;
import com.bidnet.server.engine.BidProcessor;
import com.bidnet.server.engine.TimerManager;
import com.bidnet.server.network.BroadcastManager;
import com.bidnet.server.network.ClientHandler;
import com.bidnet.server.network.ConnectionManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserializes and dispatches incoming messages to appropriate services.
 * Reference: PDF Slides 20, 21, 49, 56, 57.
 */
public class MessageRouter {
    private static final Logger logger = LoggerFactory.getLogger(MessageRouter.class);
    private static final MessageRouter instance = new MessageRouter();

    public static MessageRouter getInstance() {
        return instance;
    }

    public void route(ClientHandler handler, String rawJson) {
        try {
            Message msg = Message.fromJson(rawJson);
            if (msg == null || msg.getType() == null) {
                logger.warn("Received malformed or null message: {}", rawJson);
                return;
            }

            switch (msg.getType()) {
                case LOGIN:
                    AuthService.handleLogin(handler, msg);
                    break;

                case PLACE_BID:
                    BidProcessor.process(msg);
                    break;

                case JOIN_AUCTION:
                    AuctionManager.joinRoom(msg.getSenderId(), msg.getAuctionId());
                    break;

                case LEAVE_AUCTION:
                    AuctionManager.leaveRoom(msg.getSenderId(), msg.getAuctionId());
                    break;

                case AUCTION_START:
                    handleAuctionStart(handler, msg);
                    break;

                case HEARTBEAT_ACK:
                    handler.recordHeartbeatAck();
                    break;

                case RECONNECT:
                    handleReconnect(handler, msg);
                    break;

                default:
                    logger.warn("Unknown command received: {}", msg.getType());
                    System.out.println("Unknown command: " + msg.getType());
                    break;
            }
        } catch (Exception e) {
            logger.error("Error processing message [{}]: {}", rawJson, e.getMessage());
            Message errorMsg = new Message(MessageType.ERROR, "SERVER", null, "{\"error\":\"" + e.getMessage() + "\"}");
            handler.sendMessage(errorMsg.toJson());
        }
    }

    private void handleAuctionStart(ClientHandler handler, Message msg) {
        // Admin operations (Slide 49)
        String auctionId = msg.getAuctionId();
        Auction auction = AuctionManager.getAuction(auctionId);
        if (auction != null) {
            auction.setActive(true);
            long durationSec = 60; // default 60s countdown
            try {
                if (msg.getPayload() != null && !msg.getPayload().isEmpty()) {
                    JsonObject payload = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
                    if (payload.has("durationSec")) {
                        durationSec = payload.get("durationSec").getAsLong();
                    }
                }
            } catch (Exception ignored) {}

            TimerManager.scheduleEnd(auction, durationSec);
            BroadcastManager.broadcastUpdate(auction);
            logger.info("Admin started auction {} with duration {}s", auctionId, durationSec);
        }
    }

    private void handleReconnect(ClientHandler handler, Message msg) {
        // Reconnect protocol (Slide 56, 57)
        String userId = msg.getSenderId();
        ConnectionManager.getInstance().addClient(userId, handler);
        if (msg.getAuctionId() != null) {
            AuctionManager.joinRoom(userId, msg.getAuctionId());
        }
        logger.info("Client {} successfully resumed session", userId);
    }
}
