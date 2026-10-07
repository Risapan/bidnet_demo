package com.bidnet.server;

import com.bidnet.server.protocol.Message;
import com.bidnet.server.protocol.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Protocol framing and JSON serialization unit tests.
 * Reference: PDF Slides 7, 8, 17, 18, 19.
 */
public class ProtocolTest {

    @Test
    public void testLoginPayloadSerialization() {
        Message loginMsg = new Message(MessageType.LOGIN, "U123");
        loginMsg.setPayload("{\"username\":\"admin\",\"password\":\"1234\"}");

        String json = loginMsg.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"type\":\"LOGIN\""));
        assertTrue(json.contains("\"senderId\":\"U123\""));

        Message parsed = Message.fromJson(json);
        assertEquals(MessageType.LOGIN, parsed.getType());
        assertEquals("U123", parsed.getSenderId());
        assertEquals("{\"username\":\"admin\",\"password\":\"1234\"}", parsed.getPayload());
    }

    @Test
    public void testPlaceBidPayloadSerialization() {
        Message bidMsg = new Message(MessageType.PLACE_BID, "U123", "A-999", "{\"amount\": 250000.0}");

        String json = bidMsg.toJson();
        Message parsed = Message.fromJson(json);

        assertEquals(MessageType.PLACE_BID, parsed.getType());
        assertEquals("U123", parsed.getSenderId());
        assertEquals("A-999", parsed.getAuctionId());
        assertEquals("{\"amount\": 250000.0}", parsed.getPayload());
    }

    @Test
    public void testAuctionUpdatePayloadSerialization() {
        Message updateMsg = new Message(MessageType.AUCTION_UPDATE, "SERVER", "A-999",
                "{\"currentBid\":250000.0, \"highestBidder\":\"U123\"}");

        String json = updateMsg.toJson();
        Message parsed = Message.fromJson(json);

        assertEquals(MessageType.AUCTION_UPDATE, parsed.getType());
        assertEquals("SERVER", parsed.getSenderId());
        assertEquals("A-999", parsed.getAuctionId());
    }
}
