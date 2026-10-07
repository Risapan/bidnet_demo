package com.bidnet.server.protocol;

import com.google.gson.Gson;

/**
 * Universal DTO for TCP serialization.
 * Reference: PDF Slides 7, 8.
 */
public class Message {
    private MessageType type;
    private String senderId;
    private String auctionId;
    private String payload; // JSON embedded

    public Message() {}

    public Message(MessageType type, String senderId) {
        this.type = type;
        this.senderId = senderId;
    }

    public Message(MessageType type, String senderId, String auctionId, String payload) {
        this.type = type;
        this.senderId = senderId;
        this.auctionId = auctionId;
        this.payload = payload;
    }

    // Getters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    // Convert to JSON String for TCP transmission
    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Message fromJson(String json) {
        return new Gson().fromJson(json, Message.class);
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", senderId='" + senderId + '\'' +
                ", auctionId='" + auctionId + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
