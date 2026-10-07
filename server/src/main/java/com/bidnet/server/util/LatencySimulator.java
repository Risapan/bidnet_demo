package com.bidnet.server.util;

/**
 * Utility to simulate network jitter and latency in distributed environments.
 * Reference: PDF Slide 64.
 */
public class LatencySimulator {
    private static volatile boolean enabled = false;
    private static volatile long delayMs = 50;

    public static void setEnabled(boolean enable) {
        enabled = enable;
    }

    public static void setDelayMs(long delay) {
        delayMs = delay;
    }

    public static void simulateJitter() {
        if (enabled && delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
