package com.bidnet.client.ui;

import com.bidnet.client.network.BidNetClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for client authentication and server connection.
 */
public class LoginController {

    @FXML private TextField txtHost;
    @FXML private TextField txtPort;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtAuctionId;
    @FXML private Label lblError;

    private BidNetClient client;

    @FXML
    public void initialize() {
        txtHost.setText("127.0.0.1");
        txtPort.setText("5000");
        txtAuctionId.setText("A-999");
    }

    @FXML
    public void onLoginClicked() {
        String host = txtHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            lblError.setText("Invalid port number");
            return;
        }

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String auctionId = txtAuctionId.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblError.setText("Username and password required");
            return;
        }

        client = new BidNetClient();
        try {
            client.connect(host, port);
            client.startListening();

            // Set temporary listener for LOGIN_RESPONSE
            client.setListener(rawJson -> {
                try {
                    JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
                    String type = json.get("type").getAsString();

                    if ("LOGIN_RESPONSE".equals(type)) {
                        String payloadStr = json.get("payload").getAsString();
                        JsonObject payload = JsonParser.parseString(payloadStr).getAsJsonObject();
                        boolean success = payload.get("success").getAsBoolean();

                        if (success) {
                            boolean isAdmin = payload.has("isAdmin") && payload.get("isAdmin").getAsBoolean();
                            Platform.runLater(() -> openAuctionView(username, auctionId, isAdmin));
                        } else {
                            String err = payload.has("error") ? payload.get("error").getAsString() : "Login failed";
                            Platform.runLater(() -> lblError.setText(err));
                        }
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> lblError.setText("Response parsing error: " + e.getMessage()));
                }
            });

            // Send LOGIN request (Slide 17)
            String loginPayload = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
            String loginMsg = String.format("{\"type\":\"LOGIN\",\"senderId\":\"%s\",\"payload\":\"%s\"}",
                    username, loginPayload.replace("\"", "\\\""));
            client.sendMessage(loginMsg);

        } catch (Exception e) {
            lblError.setText("Connection failed: " + e.getMessage());
        }
    }

    private void openAuctionView(String user, String auctionId, boolean isAdmin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bidnet/client/ui/auction_view.fxml"));
            Parent root = loader.load();

            AuctionController controller = loader.getController();
            controller.initData(client, user, auctionId, isAdmin);

            Stage stage = (Stage) txtUsername.getScene().getWindow();
            stage.setTitle("BidNet - " + user + " (" + (isAdmin ? "Admin" : "Bidder") + ")");
            stage.setScene(new Scene(root, 650, 480));
            stage.show();
        } catch (Exception e) {
            lblError.setText("Error loading auction room: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
