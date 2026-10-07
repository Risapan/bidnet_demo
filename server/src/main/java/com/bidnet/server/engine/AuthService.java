package com.bidnet.server.engine;

import com.bidnet.server.db.UserRepository;
import com.bidnet.server.network.ClientHandler;
import com.bidnet.server.network.ConnectionManager;
import com.bidnet.server.protocol.Message;
import com.bidnet.server.protocol.MessageType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication service handling login, BCrypt password hashing, and user registration.
 * Reference: PDF Slides 22, 41, 49.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public static void handleLogin(ClientHandler handler, Message msg) {
        String username = "guest";
        String password = "";

        try {
            if (msg.getPayload() != null && !msg.getPayload().isEmpty()) {
                JsonObject json = JsonParser.parseString(msg.getPayload()).getAsJsonObject();
                if (json.has("username")) username = json.get("username").getAsString();
                if (json.has("password")) password = json.get("password").getAsString();
            }
        } catch (Exception e) {
            logger.warn("Error parsing login payload: {}", e.getMessage());
        }

        String userId = msg.getSenderId();
        if (userId == null || userId.trim().isEmpty()) {
            userId = username;
        }

        // Authenticate user via DB or mock credentials (BCrypt hashed)
        boolean authenticated = authenticateUser(username, password);
        boolean isAdmin = "admin".equalsIgnoreCase(username);

        if (authenticated) {
            ConnectionManager.getInstance().addClient(userId, handler);

            JsonObject responsePayload = new JsonObject();
            responsePayload.addProperty("success", true);
            responsePayload.addProperty("userId", userId);
            responsePayload.addProperty("username", username);
            responsePayload.addProperty("isAdmin", isAdmin);

            Message response = new Message(MessageType.LOGIN_RESPONSE, "SERVER", null, responsePayload.toString());
            handler.sendMessage(response.toJson());
            logger.info("Authentication successful for user: {} (admin={})", userId, isAdmin);
        } else {
            JsonObject responsePayload = new JsonObject();
            responsePayload.addProperty("success", false);
            responsePayload.addProperty("error", "Invalid credentials");

            Message response = new Message(MessageType.LOGIN_RESPONSE, "SERVER", null, responsePayload.toString());
            handler.sendMessage(response.toJson());
            logger.warn("Authentication failed for user: {}", username);
        }
    }

    private static boolean authenticateUser(String username, String password) {
        // Query MongoDB repository first (Slide 22 & 41)
        String storedHash = UserRepository.getPasswordHash(username);
        if (storedHash != null) {
            try {
                return BCrypt.checkpw(password, storedHash);
            } catch (Exception e) {
                logger.error("BCrypt validation failed: {}", e.getMessage());
            }
        }

        // Default credentials for testing and university demos:
        // admin / 1234 or any demo user
        if ("admin".equals(username) && "1234".equals(password)) {
            return true;
        }
        // Allow demo bidders (e.g. U1, U2, U3, guest)
        return password != null && !password.trim().isEmpty();
    }
}
