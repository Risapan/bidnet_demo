package com.bidnet.server;

import com.bidnet.server.network.BidNetServer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end distributed system integration test.
 * Boots server, opens live TCP socket connection, executes login handshake,
 * joins an auction, places competitive bids, and verifies broadcast distribution.
 */
public class EndToEndIntegrationTest {

    private static final int TEST_PORT = 5088;
    private static BidNetServer server;
    private static Thread serverThread;

    @BeforeAll
    public static void startServer() throws Exception {
        server = new BidNetServer(TEST_PORT);
        serverThread = new Thread(() -> server.start(), "E2E-BidNetServer");
        serverThread.setDaemon(true);
        serverThread.start();
        server.awaitBound(5, TimeUnit.SECONDS);
    }

    @AfterAll
    public static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testLiveTcpClientWorkflow() throws Exception {
        CountDownLatch loginLatch = new CountDownLatch(1);
        CountDownLatch bidAcceptedLatch = new CountDownLatch(1);
        CountDownLatch updateReceivedLatch = new CountDownLatch(1);
        AtomicBoolean gotAuctionUpdate = new AtomicBoolean(false);

        try (Socket socket = new Socket("127.0.0.1", TEST_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            // Receiver thread for client
            Thread receiver = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                        String type = json.get("type").getAsString();

                        if ("LOGIN_RESPONSE".equals(type)) {
                            loginLatch.countDown();
                        } else if ("BID_ACCEPTED".equals(type)) {
                            bidAcceptedLatch.countDown();
                        } else if ("AUCTION_UPDATE".equals(type)) {
                            String pStr = json.get("payload").getAsString();
                            JsonObject payload = JsonParser.parseString(pStr).getAsJsonObject();
                            if (payload.has("currentBid") && payload.get("currentBid").getAsDouble() >= 250000.0) {
                                gotAuctionUpdate.set(true);
                                updateReceivedLatch.countDown();
                            }
                        }
                    }
                } catch (Exception ignored) {}
            });
            receiver.setDaemon(true);
            receiver.start();

            // 1. Send Login
            out.println("{\"type\":\"LOGIN\",\"senderId\":\"U123\",\"payload\":\"{\\\"username\\\":\\\"admin\\\",\\\"password\\\":\\\"1234\\\"}\"}");
            assertTrue(loginLatch.await(3, TimeUnit.SECONDS), "Server must respond with LOGIN_RESPONSE");

            // 2. Join Auction
            out.println("{\"type\":\"JOIN_AUCTION\",\"senderId\":\"U123\",\"auctionId\":\"A-999\"}");

            // 3. Place Bid
            out.println("{\"type\":\"PLACE_BID\",\"senderId\":\"U123\",\"auctionId\":\"A-999\",\"payload\":\"{\\\"amount\\\":250000.0}\"}");
            assertTrue(bidAcceptedLatch.await(3, TimeUnit.SECONDS), "Server must acknowledge bid with BID_ACCEPTED");
            assertTrue(updateReceivedLatch.await(3, TimeUnit.SECONDS), "Server must push AUCTION_UPDATE broadcast");
            assertTrue(gotAuctionUpdate.get(), "Auction update must reflect the accepted bid amount");
        }
    }
}
