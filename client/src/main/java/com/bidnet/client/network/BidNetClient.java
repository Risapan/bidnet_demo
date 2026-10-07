package com.bidnet.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Client Application Core for TCP Socket connection and real-time streaming.
 * Reference: PDF Slides 36, 37, 38.
 */
public class BidNetClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private MessageListener listener;
    private volatile boolean isListening;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        isListening = true;
    }

    public void startListening() {
        new Thread(() -> {
            try {
                String line;
                while (isListening && (line = in.readLine()) != null) {
                    if (listener != null) {
                        listener.onMessageReceived(line);
                    }
                }
            } catch (IOException e) {
                if (isListening) {
                    System.out.println("Disconnected from server: " + e.getMessage());
                }
            } finally {
                disconnect();
            }
        }, "BidNet-Client-Receiver").start();
    }

    public synchronized void sendMessage(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
        }
    }

    public void setListener(MessageListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void disconnect() {
        isListening = false;
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
