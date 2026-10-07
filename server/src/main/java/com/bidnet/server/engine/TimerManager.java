package com.bidnet.server.engine;

import com.bidnet.server.db.AuctionRepository;
import com.bidnet.server.network.BroadcastManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles auction countdowns and automatic closure.
 * Reference: PDF Slide 30.
 */
public class TimerManager {
    private static final Logger logger = LoggerFactory.getLogger(TimerManager.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public static void scheduleEnd(Auction auction, long delaySec) {
        logger.info("Scheduling auction {} to close in {} seconds", auction.getAuctionId(), delaySec);
        scheduler.schedule(() -> {
            try {
                auction.endAuction();
                logger.info("Auction {} concluded! Winner: {} with Rs. {}",
                        auction.getAuctionId(), auction.getHighestBidder(), auction.getCurrentBid());

                // Broadcast conclusion to all connected clients
                BroadcastManager.broadcastEnd(auction);

                // Persist final winner to database (Slide 34)
                AuctionRepository.saveResult(auction.getAuctionId(), auction.getHighestBidder(), auction.getCurrentBid());
            } catch (Exception e) {
                logger.error("Error finalizing auction {}: {}", auction.getAuctionId(), e.getMessage());
            }
        }, delaySec, TimeUnit.SECONDS);
    }
}
