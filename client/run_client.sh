#!/bin/bash
# BidNet Client Simulation Script
# Reference: PDF Slide 45

export JAVA_HOME="/opt/homebrew/Cellar/openjdk/27/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=================================================="
echo "          Building BidNet Client...               "
echo "=================================================="
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

mvn clean compile

echo "=================================================="
echo "     Starting 3 Client Instances...               "
echo "=================================================="
mvn exec:java -Dexec.mainClass="com.bidnet.client.ui.Main" -Dexec.args="127.0.0.1 5000 User1 A-999" &
sleep 1
mvn exec:java -Dexec.mainClass="com.bidnet.client.ui.Main" -Dexec.args="127.0.0.1 5000 User2 A-999" &
sleep 1
mvn exec:java -Dexec.mainClass="com.bidnet.client.ui.Main" -Dexec.args="127.0.0.1 5000 User3 A-999" &

wait
