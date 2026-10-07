package com.bidnet.server.network;

import com.bidnet.server.protocol.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Manages individual socket lifecycles in a dedicated worker thread.
 * Reference: PDF Slides 11, 12, 13, 46.
 */
public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientId;
    private volatile boolean isRunning = true;
    private volatile long lastHeartbeatAck = System.currentTimeMillis();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            logger.info("Client connected from {}", socket.getRemoteSocketAddress());

            String rawMessage;
            while (isRunning && (rawMessage = in.readLine()) != null) {
                if (!rawMessage.trim().isEmpty()) {
                    processMessage(rawMessage);
                }
            }
        } catch (SocketException e) {
            // TCP streams can throw SocketException on abrupt disconnect (Slide 46)
            logger.warn("Abrupt disconnect detected for client {}: {}", clientId, e.getMessage());
        } catch (IOException e) {
            logger.error("I/O error handling client {}: {}", clientId, e.getMessage());
        } finally {
            handleDisconnect();
        }
    }

    private void processMessage(String rawJson) {
        // Forward to central dispatcher (Slide 13)
        MessageRouter.getInstance().route(this, rawJson);
    }

    public synchronized void sendMessage(String json) {
        if (out != null) {
            out.println(json); // Frame delimited by newline (Slide 13)
        }
    }

    public void handleDisconnect() {
        if (!isRunning) return;
        isRunning = false;
        logger.info("Unregistering and closing socket for client: {}", clientId);
        ConnectionManager.getInstance().removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.debug("Error while closing socket: {}", e.getMessage());
        }
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getLastHeartbeatAck() {
        return lastHeartbeatAck;
    }

    public void recordHeartbeatAck() {
        this.lastHeartbeatAck = System.currentTimeMillis();
    }

    public Socket getSocket() {
        return socket;
    }
}
