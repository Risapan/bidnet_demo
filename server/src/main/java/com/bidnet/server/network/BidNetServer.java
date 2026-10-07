package com.bidnet.server.network;

import com.bidnet.server.db.MongoDbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Core TCP Server for BidNet.
 * Binds port 5000 and dispatches clients into a 100-thread fixed thread pool.
 * Reference: PDF Slides 9, 10, 54.
 */
public class BidNetServer {
    private static final Logger logger = LoggerFactory.getLogger(BidNetServer.class);
    public static final int DEFAULT_PORT = 5000;
    private int port;

    // Thread pool to handle up to 100 concurrent users (Slide 9, 54)
    private final ExecutorService threadPool;
    private volatile boolean isRunning;
    private ServerSocket serverSocket;
    private final CountDownLatch bindLatch = new CountDownLatch(1);

    public BidNetServer() {
        this(Integer.getInteger("bidnet.port", DEFAULT_PORT));
    }

    public BidNetServer(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(100);
        this.isRunning = true;
    }

    public void awaitBound(long timeout, TimeUnit unit) throws InterruptedException {
        bindLatch.await(timeout, unit);
    }

    public void start() {
        try {
            serverSocket = createServerSocket(port);
            port = serverSocket.getLocalPort();
            bindLatch.countDown();

            logger.info("BidNet Server successfully listening on port {}", port);
            System.out.println("==================================================");
            System.out.println("   BidNet Server listening on port " + port);
            System.out.println("   Thread pool capacity: 100 concurrent clients");
            System.out.println("==================================================");

            // Initialize database connection (with fast in-memory fallback)
            MongoDbManager.connect();

            // Launch background heartbeat daemon
            HeartbeatService.start();

            // Shutdown hook for graceful server shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            // The infinite accept loop (Slide 10)
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New incoming TCP connection from: {}", clientSocket.getInetAddress());
                System.out.println("New connection: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }
        } catch (IOException e) {
            if (isRunning) {
                logger.error("ServerSocket exception on port {}: {}", port, e.getMessage());
            }
        } finally {
            stop();
        }
    }

    private ServerSocket createServerSocket(int targetPort) throws IOException {
        try {
            return new ServerSocket(targetPort);
        } catch (java.net.BindException e) {
            if (targetPort == DEFAULT_PORT) {
                logger.warn("Port 5000 in use (e.g., macOS AirPlay Receiver). Retrying on fallback port 5050...");
                System.out.println("Port 5000 occupied by OS. Falling back to port 5050...");
                return new ServerSocket(5050);
            }
            throw e;
        }
    }

    public synchronized void stop() {
        if (!isRunning) return;
        this.isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        this.threadPool.shutdownNow();
    }

    public int getPort() {
        return port;
    }

    public static void main(String[] args) {
        BidNetServer server = new BidNetServer();
        server.start();
    }
}
