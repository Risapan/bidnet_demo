#!/bin/bash
# BidNet One-Click Quickstart Demo Script
# Runs the server in the background and opens an interactive bidding console.

export JAVA_HOME="/opt/homebrew/Cellar/openjdk/27/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT" || exit 1

echo "=========================================================="
echo "          Starting BidNet Real-Time Auction System         "
echo "=========================================================="

# Check if port 5000 is occupied by macOS AirPlay, choose port
PORT=5000
if lsof -i :5000 >/dev/null 2>&1; then
    echo "Notice: Port 5000 is occupied (e.g., macOS AirPlay). Using port 5050."
    PORT=5050
fi

# Clean up any previous server instance
pkill -f "com.bidnet.server.network.BidNetServer" 2>/dev/null

echo "1. Starting BidNet Server on port $PORT..."
cd "$PROJECT_ROOT/server" || exit 1
mvn exec:java -Dexec.mainClass="com.bidnet.server.network.BidNetServer" -Dbidnet.port=$PORT > server.log 2>&1 &
SERVER_PID=$!
echo "   Server PID: $SERVER_PID (Logging to server/server.log)"

# Wait for server to bind
echo "   Waiting for server to initialize..."
sleep 3

echo ""
echo "=========================================================="
echo "2. Launching Interactive Bidding Client (User: Bidder1)"
echo "   - You can enter a bid amount (e.g. 210000) and press Enter"
echo "   - To exit, type 'quit'"
echo "=========================================================="
echo ""

cd "$PROJECT_ROOT/client" || exit 1
mvn exec:java -Dexec.mainClass="com.bidnet.client.ui.ConsoleBidder" -Dexec.args="127.0.0.1 $PORT Bidder1 A-999"

# Cleanup on exit
echo ""
echo "Stopping server process..."
kill $SERVER_PID 2>/dev/null
echo "Done."
