#!/bin/bash

echo "=========================================================="
echo "   AI Log Analyzer - Deployment Verification Script"
echo "=========================================================="

# 1. Check Docker Services
echo "[+] Checking Infrastructure (Docker)..."
if docker compose -f docker/docker-compose.yml ps | grep "Up"; then
    echo "SUCCESS: Docker services seem to be running."
else
    echo "WARNING: Docker services might be down. Run: docker compose -f docker/docker-compose.yml up -d"
fi

echo ""

# 2. Check Service Build
echo "[+] Verifying Service Build..."
if [ -f log-analyzer-service/target/log-analyzer-service-0.0.1-SNAPSHOT.jar ]; then
    echo "SUCCESS: Log Analyzer Service JAR found."
else
    echo "ERROR: Log Analyzer Service JAR not found. Run: mvn clean install -pl log-analyzer-service"
fi

if [ -f log-producer-simulator/target/log-producer-simulator-0.0.1-SNAPSHOT.jar ]; then
    echo "SUCCESS: Log Producer Simulator JAR found."
else
    echo "ERROR: Log Producer Simulator JAR not found. Run: mvn clean install -pl log-producer-simulator"
fi

echo ""
echo "=========================================================="
echo "   To Run Integration Test:"
echo "=========================================================="
echo "1. Start Infrastructure:  docker compose -f docker/docker-compose.yml up -d"
echo "2. Start Analyzer:        java -jar log-analyzer-service/target/log-analyzer-service-0.0.1-SNAPSHOT.jar"
echo "3. Start Simulator:       java -jar log-producer-simulator/target/log-producer-simulator-0.0.1-SNAPSHOT.jar"
echo ""
echo "4. Watch logs for '[ALERT] NEW INCIDENT DETECTED'"
echo "=========================================================="
