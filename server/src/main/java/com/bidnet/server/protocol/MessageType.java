package com.bidnet.server.protocol;

/**
 * Strongly typed message types for network traffic in BidNet.
 * Reference: PDF Slide 6, 49, 57.
 */
public enum MessageType {
    LOGIN,
    LOGIN_RESPONSE,
    JOIN_AUCTION,
    LEAVE_AUCTION,
    PLACE_BID,
    BID_ACCEPTED,
    BID_REJECTED,
    AUCTION_UPDATE,
    AUCTION_START,
    HEARTBEAT,
    HEARTBEAT_ACK,
    RECONNECT,
    ERROR
}
