package com.bidnet.server;

import com.bidnet.server.engine.Auction;
import com.bidnet.server.engine.AuctionManager;
import com.bidnet.server.engine.BidProcessor;
import com.bidnet.server.protocol.Message;
import com.bidnet.server.protocol.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit testing for auction domain validation rules and 5% increment constraint.
 * Reference: PDF Slides 26, 38, 58, 59.
 */
public class AuctionValidationTest {

    private Auction auction;

    @BeforeEach
    public void setup() {
        auction = new Auction("TEST-AUC-1", "Gaming Laptop", 1000.0);
        AuctionManager.addAuction(auction);
    }

    @Test
    public void testFirstBidAtStartPriceAllowed() {
        Message bidMsg = new Message(MessageType.PLACE_BID, "User1", "TEST-AUC-1", "{\"amount\": 1000.0}");
        BidProcessor.process(bidMsg);

        assertEquals(1000.0, auction.getCurrentBid(), 0.001);
        assertEquals("User1", auction.getHighestBidder());
    }

    @Test
    public void testFivePercentMinimumIncrementEnforced() {
        // Place initial bid
        Message bid1 = new Message(MessageType.PLACE_BID, "User1", "TEST-AUC-1", "{\"amount\": 1000.0}");
        BidProcessor.process(bid1);

        // Required next bid must be at least 1000 + (1000 * 0.05) = 1050.0
        // Attempt a bid of 1030 (only 3% increment) -> must be rejected
        Message bidTooLow = new Message(MessageType.PLACE_BID, "User2", "TEST-AUC-1", "{\"amount\": 1030.0}");
        BidProcessor.process(bidTooLow);

        // State remains at User1 and 1000.0
        assertEquals(1000.0, auction.getCurrentBid(), 0.001);
        assertEquals("User1", auction.getHighestBidder());

        // Now place valid bid of 1060 (6% increment) -> must be accepted
        Message validBid = new Message(MessageType.PLACE_BID, "User2", "TEST-AUC-1", "{\"amount\": 1060.0}");
        BidProcessor.process(validBid);

        assertEquals(1060.0, auction.getCurrentBid(), 0.001);
        assertEquals("User2", auction.getHighestBidder());
    }

    @Test
    public void testNegativeAndZeroBidsRejected() {
        Message negativeBid = new Message(MessageType.PLACE_BID, "UserX", "TEST-AUC-1", "{\"amount\": -500.0}");
        BidProcessor.process(negativeBid);
        assertEquals(1000.0, auction.getCurrentBid(), 0.001);

        Message zeroBid = new Message(MessageType.PLACE_BID, "UserX", "TEST-AUC-1", "{\"amount\": 0.0}");
        BidProcessor.process(zeroBid);
        assertEquals(1000.0, auction.getCurrentBid(), 0.001);
    }

    @Test
    public void testClosedAuctionRejection() {
        auction.endAuction();
        assertFalse(auction.isActive());

        Message bidOnClosed = new Message(MessageType.PLACE_BID, "UserY", "TEST-AUC-1", "{\"amount\": 5000.0}");
        BidProcessor.process(bidOnClosed);

        // Should not update
        assertEquals(1000.0, auction.getCurrentBid(), 0.001);
    }
}
