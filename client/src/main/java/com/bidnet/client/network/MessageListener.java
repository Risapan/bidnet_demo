package com.bidnet.client.network;

/**
 * Functional callback interface for receiving asynchronous TCP JSON events from BidNet server.
 * Reference: PDF Slide 38.
 */
@FunctionalInterface
public interface MessageListener {
    void onMessageReceived(String message);
}
