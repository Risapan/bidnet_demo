# BidNet Wire Protocol Specification
**Course**: EC5205: Operating Systems & Network Programming  
**Appendix A**: JSON Strings & Wire Framing (Reference: PDF Slide 91)

---

## 1. Framing Convention
- **Transport**: Raw TCP Socket (`java.net.Socket`, `java.net.ServerSocket`)
- **Port**: `5000`
- **Encoding**: UTF-8
- **Framing**: Line-delimited JSON (`\n`). Every packet is a single-line valid JSON object.

---

## 2. Universal Message Envelope
```json
{
  "type": "MESSAGE_TYPE_ENUM",
  "senderId": "STRING_USER_ID",
  "auctionId": "STRING_AUCTION_ID",
  "payload": "STRINGIFIED_NESTED_JSON_OBJECT"
}
```

---

## 3. Protocol Message Catalog

### 3.1 Authentication
#### `LOGIN` (Client -> Server)
Sent immediately after establishing TCP connection.
```json
{
  "type": "LOGIN",
  "senderId": "U123",
  "payload": "{\"username\":\"admin\",\"password\":\"1234\"}"
}
```

#### `LOGIN_RESPONSE` (Server -> Client)
Server validates credentials (using BCrypt) and registers the socket in `ConnectionManager`.
```json
{
  "type": "LOGIN_RESPONSE",
  "senderId": "SERVER",
  "payload": "{\"success\":true,\"userId\":\"U123\",\"username\":\"admin\",\"isAdmin\":true}"
}
```

---

### 3.2 Auction Subscriptions & Lifecycle
#### `JOIN_AUCTION` (Client -> Server)
Subscribes the user to real-time broadcasts for a specific room.
```json
{
  "type": "JOIN_AUCTION",
  "senderId": "U123",
  "auctionId": "A-999"
}
```

#### `LEAVE_AUCTION` (Client -> Server)
Unsubscribes the user from the broadcast room.
```json
{
  "type": "LEAVE_AUCTION",
  "senderId": "U123",
  "auctionId": "A-999"
}
```

#### `AUCTION_START` (Client -> Server, Admin Only)
Starts an auction countdown timer.
```json
{
  "type": "AUCTION_START",
  "senderId": "admin",
  "auctionId": "A-999",
  "payload": "{\"durationSec\":60}"
}
```

---

### 3.3 Bidding
#### `PLACE_BID` (Client -> Server)
Submits a proposed bid. Handled under fair `ReentrantLock` in `BidProcessor`.
```json
{
  "type": "PLACE_BID",
  "senderId": "U123",
  "auctionId": "A-999",
  "payload": "{\"amount\": 250000.0}"
}
```

#### `BID_ACCEPTED` (Server -> Client)
Sent strictly to the winning bidder acknowledging their bid.
```json
{
  "type": "BID_ACCEPTED",
  "senderId": "SERVER",
  "auctionId": "A-999",
  "payload": "{\"status\":\"ACCEPTED\",\"amount\":250000.0}"
}
```

#### `BID_REJECTED` (Server -> Client)
Sent to the rejected bidder with cause (`BID_TOO_LOW`, `AUCTION_CLOSED`, `INVALID_BID_AMOUNT`).
```json
{
  "type": "BID_REJECTED",
  "senderId": "SERVER",
  "auctionId": "A-999",
  "payload": "{\"status\":\"REJECTED\",\"reason\":\"BID_TOO_LOW\"}"
}
```

#### `AUCTION_UPDATE` (Server -> Broadcast All Subscribers)
Multicast to all subscribed clients when price changes or auction ends.
```json
{
  "type": "AUCTION_UPDATE",
  "senderId": "SERVER",
  "auctionId": "A-999",
  "payload": "{\"currentBid\":250000.0,\"highestBidder\":\"U123\",\"isActive\":true,\"title\":\"MacBook Pro M3 Max\"}"
}
```

---

### 3.4 Connection Maintenance & Reconnect
#### `HEARTBEAT` (Server -> Client)
Periodic liveness query sent every 10 seconds.
```json
{
  "type": "HEARTBEAT"
}
```

#### `HEARTBEAT_ACK` (Client -> Server)
Sent by active clients. Lack of response within 30s triggers server-side disconnect.
```json
{
  "type": "HEARTBEAT_ACK",
  "senderId": "U123"
}
```

#### `RECONNECT` (Client -> Server)
Allows dropped connections to restore active subscriptions.
```json
{
  "type": "RECONNECT",
  "senderId": "U123",
  "auctionId": "A-999"
}
```
