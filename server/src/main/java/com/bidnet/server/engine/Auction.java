package com.bidnet.server.engine;

import java.util.concurrent.locks.ReentrantLock;

/**
 * The central state object representing an active or completed auction.
 * Features strict per-item concurrency control using ReentrantLock with fairness enabled.
 * Reference: PDF Slides 23, 24, 25, 42, 50, 62.
 */
public class Auction {
    private final String auctionId;
    private final String title;
    private double currentBid;
    private String highestBidderId;
    private boolean isActive;

    // Concurrency control for THIS specific auction.
    // true = fair queueing (FIFO) to prevent thread starvation under heavy load.
    private final ReentrantLock lock = new ReentrantLock(true);

    public Auction(String id, String title, double startPrice) {
        this.auctionId = id;
        this.title = title;
        this.currentBid = startPrice;
        this.highestBidderId = "None";
        this.isActive = true;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getTitle() {
        return title;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public String getHighestBidder() {
        return highestBidderId;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void updateBid(double newBid, String bidderId) {
        this.currentBid = newBid;
        this.highestBidderId = bidderId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public void endAuction() {
        this.isActive = false;
    }

    public void pauseAuction() {
        // Pausing an auction sets isActive = false but preserves currentBid (Slide 50)
        this.isActive = false;
    }

    public void resumeAuction() {
        this.isActive = true;
    }

    @Override
    public String toString() {
        return "Auction{" +
                "auctionId='" + auctionId + '\'' +
                ", title='" + title + '\'' +
                ", currentBid=" + currentBid +
                ", highestBidderId='" + highestBidderId + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
