# BidNet: Real-Time Distributed Auction System
**University of Ruhuna – Faculty of Engineering**  
**EC5205: Operating Systems & Network Programming**  
*Complete Project Roadmap & Implementation*

---

## 📌 Project Overview
**BidNet** is an authoritative distributed auction system built with low-level TCP/IP stream sockets in Java. It features:
- **Custom TCP/IP Protocol**: Line-delimited JSON messaging avoiding web HTTP overhead.
- **Fair Multi-Threaded Concurrency**: Per-auction critical sections using `ReentrantLock(true)` with FIFO queueing.
- **Fixed Thread Pool**: 100-thread pool bound to prevent OS memory exhaustion.
- **Real-Time Multicast**: Pub/Sub room updates pushed immediately to all connected bidders.
- **Fault-Tolerant Persistence**: MongoDB Sync persistence with non-blocking async offloading and seamless RAM fallback mode.
- **Dual Client Interfaces**: Full modern JavaFX Dark Mode GUI and interactive Console/CLI bidder.

---

## 📁 Repository Layout
```
BidNet/
├── pom.xml                                   # Root aggregator POM
├── docs/
│   ├── protocol.md                           # JSON Wire Protocol Specification
│   └── architecture_report.md                # System Architecture & Grading Analysis
├── diagrams/
│   ├── class_diagram.mermaid                 # UML Class Model
│   └── sequence_diagram.mermaid              # Distributed Event Sequence
├── server/
│   ├── pom.xml                               # Server Maven dependencies
│   ├── run_server.sh                         # Automated server startup script
│   └── src/
│       ├── main/java/com/bidnet/server/
│       │   ├── network/                      # TCP ServerSocket, ClientHandler, ConnectionManager
│       │   ├── protocol/                     # Message, MessageType, MessageRouter
│       │   ├── engine/                       # Auction, BidProcessor, AuthService, TimerManager
│       │   ├── db/                           # MongoDbManager, Repositories (with RAM fallback)
│       │   └── util/                         # LatencySimulator
│       └── test/java/com/bidnet/server/      # Concurrency & Validation JUnit 5 Tests
└── client/
    ├── pom.xml                               # Client Maven dependencies (JavaFX 21, Gson)
    ├── run_client.sh                         # Multi-client demonstration launch script
    └── src/main/java/com/bidnet/client/
        ├── network/                          # TCP Client socket & background stream reader
        └── ui/                               # JavaFX controllers, ConsoleBidder & FXML views
```

---

## 🚀 How to Run the Demonstration (Slides 65–70)

### Step 1: Start the Server (Slide 66)
```bash
cd server
./run_server.sh
```
*Listens on port 5000 with a 100-thread pool.*

### Step 2: Run Multi-Client Bidder Simulation (Slide 68, 69)
In a separate terminal:
```bash
cd client
./run_client.sh
```
*Or launch individual console clients:*
```bash
mvn exec:java -Dexec.mainClass="com.bidnet.client.ui.ConsoleBidder" -Dexec.args="127.0.0.1 5000 UserA A-999"
mvn exec:java -Dexec.mainClass="com.bidnet.client.ui.ConsoleBidder" -Dexec.args="127.0.0.1 5000 UserB A-999"
```

### Step 3: Run the JavaFX GUI Client
```bash
cd client
mvn javafx:run
```

---

## 🧪 Running Concurrency & Unit Tests (Slide 41–43)
```bash
cd server
mvn test
```
*Verifies simultaneous race conditions, fair lock acquisition, 5% minimum bid increment validation, and payload integrity.*
