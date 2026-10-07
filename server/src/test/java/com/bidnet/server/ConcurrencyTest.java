package com.bidnet.server;

import com.bidnet.server.engine.Auction;
import com.bidnet.server.engine.AuctionManager;
import com.bidnet.server.engine.BidProcessor;
import com.bidnet.server.protocol.Message;
import com.bidnet.server.protocol.MessageType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency & Thread-safety verification suite.
 * Reference: PDF Slides 41, 42, 43.
 */
public class ConcurrencyTest {

    @Test
    public void testSimultaneousBids() throws InterruptedException {
        Auction auction = new Auction("A1", "MacBook", 1000);

        // Creating concurrent racers (Slide 42)
        Thread t1 = new Thread(() -> {
            auction.getLock().lock();
            try {
                auction.updateBid(1200, "U1");
            } finally {
                auction.getLock().unlock();
            }
        });

        Thread t2 = new Thread(() -> {
            auction.getLock().lock();
            try {
                auction.updateBid(1500, "U2");
            } finally {
                auction.getLock().unlock();
            }
        });

        // Executing the race condition test (Slide 43)
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // Highest bid should be 1200 or 1500, regardless of thread interleaving
        assertTrue(auction.getCurrentBid() == 1200 || auction.getCurrentBid() == 1500,
                "Current bid must be one of the competing values without corrupted state");
    }

    @Test
    public void testHighContentionMultiThreadedBids() throws InterruptedException {
        String testAuctionId = "A-CONCURRENCY-50";
        Auction auction = new Auction(testAuctionId, "Rare Antique Watch", 10000.0);
        AuctionManager.addAuction(auction);

        int numberOfThreads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger processedBids = new AtomicInteger(0);

        for (int i = 1; i <= numberOfThreads; i++) {
            final int index = i;
            final double bidValue = 10000.0 + (index * 1000.0);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronized thunder strike to test maximum contention
                    Message msg = new Message(MessageType.PLACE_BID, "User-" + index, testAuctionId, "{\"amount\":" + bidValue + "}");
                    BidProcessor.process(msg);
                    processedBids.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all threads at the exact same instant
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent bidding threads should complete without deadlock");
        assertEquals(numberOfThreads, processedBids.get(), "Every thread's bid should be processed through the lock");
        assertTrue(auction.getCurrentBid() >= 11000.0, "Current bid must have incremented safely");
        assertNotNull(auction.getHighestBidder(), "There must be a designated winning bidder");
    }
}
