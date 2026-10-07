package com.bidnet.client.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.GraphicsEnvironment;

/**
 * Main launcher for the BidNet Client Application.
 * Supports both JavaFX graphical mode and console/CLI fallback mode.
 * Reference: PDF Slides 39, 40, 45.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/bidnet/client/ui/login_view.fxml"));
            Parent root = loader.load();

            primaryStage.setTitle("BidNet - Distributed Auction Client");
            primaryStage.setScene(new Scene(root, 480, 420));
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Failed to launch JavaFX UI: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Falling back to Interactive Console Mode...");
            ConsoleBidder.main(new String[]{});
        }
    }

    public static void main(String[] args) {
        // If headless or --cli flag is specified, run ConsoleBidder directly
        boolean isCli = false;
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "-c".equalsIgnoreCase(arg)) {
                isCli = true;
                break;
            }
        }

        if (isCli || GraphicsEnvironment.isHeadless()) {
            ConsoleBidder.main(args);
        } else {
            try {
                launch(args);
            } catch (Throwable t) {
                System.out.println("JavaFX display not available (" + t.getMessage() + "). Starting Console Bidder...");
                ConsoleBidder.main(args);
            }
        }
    }
}
