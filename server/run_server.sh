#!/bin/bash
# BidNet Server Launch Script
# Reference: PDF Slide 44

export JAVA_HOME="/opt/homebrew/Cellar/openjdk/27/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "=================================================="
echo "          Building BidNet Server...               "
echo "=================================================="
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

mvn clean compile

echo "=================================================="
echo "     Starting BidNet Server on port 5000...       "
echo "=================================================="
mvn exec:java -Dexec.mainClass="com.bidnet.server.network.BidNetServer"
