# BidNet System Architecture & Engineering Report
**University of Ruhuna – Faculty of Engineering**  
**Course**: EC5205: Operating Systems & Network Programming  
**Authors**: Team BidNet  
**Date**: September 2026  

---

## 1. Introduction
Modern online auction systems demand determinism, minimal latency, and ironclad race-condition prevention. In environments where thousands of bidders compete down to the millisecond, standard HTTP-based REST polling introduces excessive overhead, unpredictable latency jitter, and head-of-line request queuing.

**BidNet** is designed from bare-metal TCP/IP primitives in Java. By establishing long-lived bidirectional TCP stream sockets and deploying an in-memory concurrency model backed by asynchronous MongoDB persistence, BidNet achieves sub-millisecond transaction confirmation and multi-client update multicasting.

---

## 2. Grading Criteria Mapping

| Evaluation Dimension | Operating Systems & Networking Primitives Implemented | Source Code Reference |
| :--- | :--- | :--- |
| **Networking (Slide 88)** | Custom TCP/IP server, connection pool, stream framing, non-blocking I/O buffers, socket lifecycle, keepalive heartbeats | `BidNetServer.java`, `ClientHandler.java`, `HeartbeatService.java`, `BidNetClient.java` |
| **OS Concurrency (Slide 89)** | Fair `ReentrantLock` per item, `ExecutorService` fixed pool (100 workers), `ScheduledThreadPool`, `ConcurrentHashMap`, race-free state transitions | `BidProcessor.java`, `Auction.java`, `ConnectionManager.java`, `TimerManager.java`, `ConcurrencyTest.java` |
| **Database Systems (Slide 90)** | MongoDB sync driver, asynchronous non-blocking worker persistence, fault-tolerant pure RAM fallback mode, BCrypt salted credential hashing | `MongoDbManager.java`, `AuctionRepository.java`, `UserRepository.java` |

---

## 3. Operating Systems Concurrency Analysis

### 3.1 Thread Pool Configuration
Rather than spawning an unbounded thread per incoming socket (`newCachedThreadPool()`), which exposes the JVM to memory exhaustion and thread thrashing under load spikes (Slide 54), BidNet utilizes a fixed thread pool:
```java
private final ExecutorService threadPool = Executors.newFixedThreadPool(100);
```
This guarantees an upper bound on stack memory allocation ($100 \times 1\text{MB} \approx 100\text{MB}$ heap overhead), ensuring predictable OS scheduling without CPU cache line bouncing.

### 3.2 Granular Per-Auction Locking with Fairness
Global synchronization creates catastrophic contention bottlenecks. BidNet implements item-level critical sections: each `Auction` instance encapsulates its own `ReentrantLock` instantiated with the fairness parameter set to `true`:
```java
private final ReentrantLock lock = new ReentrantLock(true);
```
- **Fairness Guarantee**: When multiple threads contend to place bids simultaneously, the JVM places waiting threads into a strict FIFO wait queue. No thread suffers starvation.
- **Deadlock Immunity**: Bid operations only ever acquire a single lock (the lock of the specific auction being bid on), eliminating circular wait conditions ($T_1 \to L_A, T_2 \to L_B$).
- **Fail-Safe Release**: All lock acquisitions are strictly guarded by `try-finally` blocks, ensuring that even unhandled exceptions or assertion failures never leak acquired locks.

---

## 4. Network Architecture & Protocol Framing

### 4.1 Custom TCP/IP Stream Protocol
HTTP introduces hundreds of bytes of header overhead per request. BidNet establishes long-lived TCP sockets utilizing line-delimited JSON frames:
$$\text{Frame} = \{\text{JSON}\} + \backslash\text{n}$$
The network reader (`ClientHandler`) processes messages in a single sweep using `BufferedReader.readLine()`.

### 4.2 Liveness & Heartbeat Watchdog
Network connections can drop abruptly without sending a TCP `FIN` packet (e.g., client crash, wifi disconnection).
BidNet deploys `HeartbeatService` running as an OS daemon thread:
- Emits `HEARTBEAT` every 10 seconds to all registered client sockets.
- Requires incoming `HEARTBEAT_ACK`.
- If no response is recorded for $\Delta t > 30\text{ seconds}$, the connection is declared dead and forcefully evicted from `ConnectionManager` to free file descriptors.

---

## 5. Persistence & Fault-Tolerant RAM Engine

### 5.1 Asynchronous Database Offloading
Writing synchronously to MongoDB within the bid critical section would tie socket broadcast latency to disk I/O latency ($5\text{ms} - 50\text{ms}$).
To prevent this, `BidProcessor` executes the bid update in RAM in microseconds, triggers the TCP broadcast immediately, and offloads MongoDB document creation to a dedicated asynchronous worker pool (`dbExecutor`).

### 5.2 In-Memory Fault Tolerance
If MongoDB crashes or network access is lost, `MongoDbManager` intercepts the failure and flips the system into pure in-memory mode. All active bids, auctions, and states continue running in JVM RAM with zero downtime.

---

## 6. JavaFX UI Thread Safety
Network streams operate on background worker threads (`Thread-0`). JavaFX forbids mutating scene-graph nodes from non-application threads (`IllegalStateException`).
`AuctionController` safely marshals all incoming updates via:
```java
Platform.runLater(() -> {
    lblCurrentBid.setText("Rs. " + bid);
    lblHighestBidder.setText(bidder);
});
```

---

## 7. Concurrency Verification & Test Results
Race-condition stress tests implemented in `ConcurrencyTest.java` prove:
1. Two simultaneous threads attempting competing bids arrive at a deterministic consistent maximum without race corruption.
2. 50 simultaneous threads contending on a single auction through a thread barrier latch execute to completion with 0 deadlocks and 100% processed bids.
