package com.bidnet.client.ui;

import com.bidnet.client.network.BidNetClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Controller updating the JavaFX UI thread safely from background TCP events.
 * Reference: PDF Slides 39, 40, 60.
 */
public class AuctionController {

    @FXML private Label lblAuctionTitle;
    @FXML private Label lblCurrentBid;
    @FXML private Label lblHighestBidder;
    @FXML private Label lblStatusMessage;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnAdminStart;

    private BidNetClient networkClient;
    private String currentUser = "U123";
    private String currentAuctionId = "A-999";
    private boolean isAdmin = false;

    public void initData(BidNetClient client, String user, String auctionId, boolean admin) {
        this.networkClient = client;
        this.currentUser = user;
        this.currentAuctionId = auctionId;
        this.isAdmin = admin;

        if (btnAdminStart != null) {
            btnAdminStart.setVisible(isAdmin);
            btnAdminStart.setManaged(isAdmin);
        }

        // Configure network listener to route incoming TCP JSON safely to JavaFX thread
        networkClient.setListener(this::handleIncomingMessage);

        // Send JOIN_AUCTION request
        String joinMsg = String.format("{\"type\":\"JOIN_AUCTION\", \"senderId\":\"%s\", \"auctionId\":\"%s\"}",
                currentUser, currentAuctionId);
        networkClient.sendMessage(joinMsg);
    }

    private void handleIncomingMessage(String rawJson) {
        try {
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            String type = json.get("type").getAsString();

            if ("AUCTION_UPDATE".equals(type)) {
                String payloadStr = json.get("payload").getAsString();
                JsonObject payload = JsonParser.parseString(payloadStr).getAsJsonObject();

                double bid = payload.has("currentBid") ? payload.get("currentBid").getAsDouble() : 0.0;
                String bidder = payload.has("highestBidder") ? payload.get("highestBidder").getAsString() : "None";
                String title = payload.has("title") ? payload.get("title").getAsString() : null;
                boolean isEnded = payload.has("status") && "AUCTION_ENDED".equals(payload.get("status").getAsString());

                Platform.runLater(() -> {
                    if (title != null && lblAuctionTitle != null) {
                        lblAuctionTitle.setText(title);
                    }
                    lblCurrentBid.setText("Rs. " + String.format("%,.2f", bid));
                    lblHighestBidder.setText(bidder);

                    if (isEnded) {
                        lblStatusMessage.setText("AUCTION ENDED! Winner: " + bidder);
                        lblStatusMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        btnPlaceBid.setDisable(true);
                    } else {
                        lblStatusMessage.setText("Live bidding in progress...");
                        lblStatusMessage.setStyle("-fx-text-fill: #2ecc71;");
                    }
                });
            } else if ("BID_ACCEPTED".equals(type)) {
                Platform.runLater(() -> {
                    lblStatusMessage.setText("✓ Your bid was accepted!");
                    lblStatusMessage.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                });
            } else if ("BID_REJECTED".equals(type)) {
                String payloadStr = json.get("payload").getAsString();
                JsonObject payload = JsonParser.parseString(payloadStr).getAsJsonObject();
                String reason = payload.has("reason") ? payload.get("reason").getAsString() : "Rejected";

                Platform.runLater(() -> {
                    lblStatusMessage.setText("✗ Bid rejected: " + reason);
                    lblStatusMessage.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                });
            } else if ("HEARTBEAT".equals(type)) {
                // Respond with HEARTBEAT_ACK
                networkClient.sendMessage("{\"type\":\"HEARTBEAT_ACK\", \"senderId\":\"" + currentUser + "\"}");
            }
        } catch (Exception e) {
            System.err.println("Error parsing client message: " + e.getMessage());
        }
    }

    public void updateUiSafe(double bid, String bidder) {
        // Must use Platform.runLater for network thread to update UI (Slide 39, 60)
        Platform.runLater(() -> {
            lblCurrentBid.setText("Rs. " + bid);
            lblHighestBidder.setText(bidder);
        });
    }

    @FXML
    public void onPlaceBidClicked() {
        try {
            double newBid = Double.parseDouble(txtBidAmount.getText().trim());
            // Construct JSON (Slide 40)
            String payload = String.format("{\"amount\":%s}", newBid);
            String msg = String.format(
                    "{\"type\":\"PLACE_BID\", \"auctionId\":\"%s\", " +
                    "\"senderId\":\"%s\", \"payload\":\"%s\"}",
                    currentAuctionId, currentUser, payload);

            networkClient.sendMessage(msg);
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            lblStatusMessage.setText("Invalid amount format");
            lblStatusMessage.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    @FXML
    public void onAdminStartClicked() {
        if (isAdmin) {
            String msg = String.format("{\"type\":\"AUCTION_START\", \"senderId\":\"%s\", \"auctionId\":\"%s\", \"payload\":\"{\\\"durationSec\\\":60}\"}",
                    currentUser, currentAuctionId);
            networkClient.sendMessage(msg);
            lblStatusMessage.setText("Auction started with 60s timer.");
        }
    }
}
