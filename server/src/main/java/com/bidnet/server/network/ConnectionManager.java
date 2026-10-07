package com.bidnet.server.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active connections globally across threads.
 * Reference: PDF Slides 14, 15, 16.
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private static ConnectionManager instance;

    // Thread-safe map of user ID to active handler
    private final ConcurrentHashMap<String, ClientHandler> clients;

    private ConnectionManager() {
        clients = new ConcurrentHashMap<>();
    }

    public static synchronized ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public void addClient(String userId, ClientHandler handler) {
        handler.setClientId(userId);
        clients.put(userId, handler);
        logger.info("User registered in ConnectionManager: {}", userId);
        System.out.println("User registered: " + userId);
    }

    public void removeClient(ClientHandler handler) {
        if (handler != null) {
            if (handler.getClientId() != null) {
                clients.remove(handler.getClientId(), handler);
            }
            clients.values().remove(handler);
        }
    }

    public ClientHandler getHandler(String userId) {
        return clients.get(userId);
    }

    public Collection<ClientHandler> getAllHandlers() {
        return clients.values();
    }

    public int getActiveCount() {
        return clients.size();
    }
}
