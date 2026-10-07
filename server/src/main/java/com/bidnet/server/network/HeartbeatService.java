package com.bidnet.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Detecting dropped TCP connections via periodic heartbeats.
 * Reference: PDF Slides 31, 32.
 */
public class HeartbeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatService.class);
    private static volatile boolean running = false;
    private static final long TIMEOUT_THRESHOLD_MS = 30000; // 30 seconds

    public static synchronized void start() {
        if (running) return;
        running = true;

        Thread heartbeatThread = new Thread(() -> {
            logger.info("HeartbeatService started (interval: 10s, timeout: 30s)");
            while (running) {
                try {
                    Thread.sleep(10000); // 10 seconds
                    pingAll();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error in heartbeat loop: {}", e.getMessage());
                }
            }
        }, "BidNet-HeartbeatService");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    public static void stop() {
        running = false;
    }

    private static void pingAll() {
        Iterable<ClientHandler> clients = ConnectionManager.getInstance().getAllHandlers();
        long now = System.currentTimeMillis();
        List<ClientHandler> timedOutClients = new ArrayList<>();

        for (ClientHandler client : clients) {
            // Check for timeout > 30s
            if (now - client.getLastHeartbeatAck() > TIMEOUT_THRESHOLD_MS) {
                timedOutClients.add(client);
            } else {
                client.sendMessage("{\"type\":\"HEARTBEAT\"}");
            }
        }

        // Clean up timed-out clients
        for (ClientHandler timedOut : timedOutClients) {
            logger.warn("Client {} exceeded 30s heartbeat timeout. Forcing disconnect.", timedOut.getClientId());
            timedOut.handleDisconnect();
        }
    }
}
