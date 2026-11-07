package com.bidnet.client.ui;

import com.bidnet.client.network.BidNetClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Run main() with the client test/runtime classpath and JavaFX controls/FXML modules. */
public class AuctionControllerBidCheck {
    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(AuctionController.class.getResource("auction_view.fxml"));
                Parent root = loader.load();
                AuctionController controller = loader.getController();
                AtomicReference<String> sent = new AtomicReference<>();
                BidNetClient client = new BidNetClient() {
                    @Override
                    public void sendMessage(String message) {
                        sent.set(message);
                    }
                };
                for (String user : new String[]{"Bidder1", "Bidder\"\\1"}) {
                    controller.initData(client, user, "A-999", false);
                    TextField amount = (TextField) root.lookup("#txtBidAmount");
                    amount.setText("210000.50");
                    controller.onPlaceBidClicked();
                    JsonObject message = JsonParser.parseString(sent.get()).getAsJsonObject();
                    JsonObject payload = JsonParser.parseString(message.get("payload").getAsString()).getAsJsonObject();
                    if (!"PLACE_BID".equals(message.get("type").getAsString())
                            || !user.equals(message.get("senderId").getAsString())
                            || !"A-999".equals(message.get("auctionId").getAsString())
                            || payload.get("amount").getAsDouble() != 210000.50) {
                        throw new AssertionError("Bid message did not preserve its fields");
                    }
                }
                result.complete(null);
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        try {
            result.get(15, TimeUnit.SECONDS);
            System.out.println("Bid JSON check passed (normal and escaped sender IDs)");
        } finally {
            Platform.exit();
        }
    }
}
