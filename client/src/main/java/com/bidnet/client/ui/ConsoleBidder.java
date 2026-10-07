package com.bidnet.client.ui;

import com.bidnet.client.network.BidNetClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * High-performance Console/CLI bidder client for multi-terminal demonstrations & automated testing.
 * Reference: PDF Slides 45, 65, 71.
 */
public class ConsoleBidder {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String user = args.length > 2 ? args[2] : "Bidder-" + (System.currentTimeMillis() % 1000);
        String auctionId = args.length > 3 ? args[3] : "A-999";

        System.out.println("==================================================");
        System.out.println("   BidNet Console Client - User: " + user);
        System.out.println("   Connecting to " + host + ":" + port + "...");
        System.out.println("==================================================");

        BidNetClient client = new BidNetClient();

        try {
            client.connect(host, port);
            client.startListening();

            client.setListener(rawJson -> {
                try {
                    JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
                    String type = json.get("type").getAsString();

                    switch (type) {
                        case "LOGIN_RESPONSE":
                            System.out.println(">> [AUTH SUCCESS] Logged in successfully as " + user);
                            // Join default auction
                            String joinMsg = String.format("{\"type\":\"JOIN_AUCTION\",\"senderId\":\"%s\",\"auctionId\":\"%s\"}",
                                    user, auctionId);
                            client.sendMessage(joinMsg);
                            break;

                        case "AUCTION_UPDATE":
                            String pStr = json.get("payload").getAsString();
                            JsonObject payload = JsonParser.parseString(pStr).getAsJsonObject();
                            double currentBid = payload.has("currentBid") ? payload.get("currentBid").getAsDouble() : 0.0;
                            String highestBidder = payload.has("highestBidder") ? payload.get("highestBidder").getAsString() : "None";
                            System.out.println("\n>> [BROADCAST] Auction " + json.get("auctionId").getAsString() +
                                    " | Current Bid: Rs. " + String.format("%,.2f", currentBid) +
                                    " | Leader: " + highestBidder);
                            System.out.print("Enter bid amount (or 'quit'): ");
                            break;

                        case "BID_ACCEPTED":
                            System.out.println("\n>> [SUCCESS] Your bid has been ACCEPTED by the server!");
                            System.out.print("Enter bid amount (or 'quit'): ");
                            break;

                        case "BID_REJECTED":
                            String rStr = json.get("payload").getAsString();
                            JsonObject rObj = JsonParser.parseString(rStr).getAsJsonObject();
                            System.out.println("\n>> [REJECTED] Bid rejected: " + rObj.get("reason").getAsString());
                            System.out.print("Enter bid amount (or 'quit'): ");
                            break;

                        case "HEARTBEAT":
                            client.sendMessage("{\"type\":\"HEARTBEAT_ACK\",\"senderId\":\"" + user + "\"}");
                            break;

                        default:
                            System.out.println(">> [" + type + "] " + rawJson);
                            break;
                    }
                } catch (Exception e) {
                    System.out.println(">> [RAW] " + rawJson);
                }
            });

            // Send login
            String loginPayload = String.format("{\"username\":\"%s\",\"password\":\"1234\"}", user);
            String loginMsg = String.format("{\"type\":\"LOGIN\",\"senderId\":\"%s\",\"payload\":\"%s\"}",
                    user, loginPayload.replace("\"", "\\\""));
            client.sendMessage(loginMsg);

            // Read bids from stdin
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if ("quit".equalsIgnoreCase(line) || "exit".equalsIgnoreCase(line)) {
                    break;
                }
                if (!line.isEmpty()) {
                    try {
                        double amount = Double.parseDouble(line);
                        String payload = String.format("{\"amount\":%s}", amount);
                        String bidMsg = String.format(
                                "{\"type\":\"PLACE_BID\",\"senderId\":\"%s\",\"auctionId\":\"%s\",\"payload\":\"%s\"}",
                                user, auctionId, payload.replace("\"", "\\\""));
                        client.sendMessage(bidMsg);
                    } catch (NumberFormatException e) {
                        System.out.println("Please enter a valid numeric amount.");
                        System.out.print("Enter bid amount: ");
                    }
                }
            }

            client.disconnect();
            System.out.println("Client exited cleanly.");

        } catch (Exception e) {
            System.err.println("Fatal connection error: " + e.getMessage());
        }
    }
}
