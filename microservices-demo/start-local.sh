#!/bin/bash
pkill -f "spring-boot:run"

echo "Starting API Gateway..."
cd api-gateway && mvn spring-boot:run > /tmp/gateway.log 2>&1 &
echo $! > /tmp/gateway.pid

echo "Starting User Service..."
cd ../user-service && mvn spring-boot:run > /tmp/user.log 2>&1 &
echo $! > /tmp/user.pid

echo "Starting Order Service..."
cd ../order-service && mvn spring-boot:run > /tmp/order.log 2>&1 &
echo $! > /tmp/order.pid

echo "Starting Inventory Service..."
cd ../inventory-service && mvn spring-boot:run > /tmp/inventory.log 2>&1 &
echo $! > /tmp/inventory.pid

echo "Starting Payment Service..."
cd ../payment-service && mvn spring-boot:run > /tmp/payment.log 2>&1 &
echo $! > /tmp/payment.pid

echo "Services starting in background. Check /tmp/*.log for details."
wait
