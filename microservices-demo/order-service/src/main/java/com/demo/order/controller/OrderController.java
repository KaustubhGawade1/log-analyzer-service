package com.demo.order.controller;

import com.demo.order.chaos.ChaosMonkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RestTemplate restTemplate;
    private final ChaosMonkey chaosMonkey;

    @Value("${services.inventory-service}")
    private String inventoryServiceUrl;

    @Value("${services.payment-service}")
    private String paymentServiceUrl;

    // Simulated order database
    private final Map<String, Map<String, Object>> orders = new ConcurrentHashMap<>();

    public OrderController(RestTemplate restTemplate, ChaosMonkey chaosMonkey) {
        this.restTemplate = restTemplate;
        this.chaosMonkey = chaosMonkey;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        String orderId = "order-" + UUID.randomUUID().toString().substring(0, 8);
        String userId = (String) request.get("userId");
        String productId = (String) request.getOrDefault("productId", "product-1");
        int quantity = (int) request.getOrDefault("quantity", 1);
        double amount = (double) request.getOrDefault("amount", 99.99);

        log.info("Creating order {} for user {} - product: {}, qty: {}", orderId, userId, productId, quantity);
        chaosMonkey.maybeInjectChaos();

        // Step 1: Reserve inventory
        log.info("Reserving inventory for order {}", orderId);
        String reserveUrl = inventoryServiceUrl + "/inventory/reserve";
        Map<String, Object> reserveRequest = Map.of(
                "productId", productId,
                "quantity", quantity,
                "orderId", orderId);
        var inventoryResponse = restTemplate.postForEntity(reserveUrl, reserveRequest, Map.class);
        log.info("Inventory reserved for order {}: {}", orderId, inventoryResponse.getBody());

        // Step 2: Process payment
        log.info("Processing payment for order {}", orderId);
        String paymentUrl = paymentServiceUrl + "/payments/process";
        Map<String, Object> paymentRequest = Map.of(
                "orderId", orderId,
                "userId", userId,
                "amount", amount);
        var paymentResponse = restTemplate.postForEntity(paymentUrl, paymentRequest, Map.class);
        log.info("Payment processed for order {}: {}", orderId, paymentResponse.getBody());

        // Create order record
        Map<String, Object> order = new ConcurrentHashMap<>();
        order.put("orderId", orderId);
        order.put("userId", userId);
        order.put("productId", productId);
        order.put("quantity", quantity);
        order.put("amount", amount);
        order.put("status", "CONFIRMED");
        order.put("createdAt", Instant.now().toString());
        order.put("inventoryReservation", inventoryResponse.getBody());
        order.put("payment", paymentResponse.getBody());

        orders.put(orderId, order);

        log.info("Order {} created successfully", orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        log.info("Fetching order: {}", orderId);
        chaosMonkey.maybeInjectChaos();

        var order = orders.get(orderId);
        if (order == null) {
            log.warn("Order not found: {}", orderId);
            return ResponseEntity.notFound().build();
        }

        log.info("Order {} found", orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<?> getOrderStatus(@PathVariable String orderId) {
        log.info("Checking status for order: {}", orderId);
        chaosMonkey.maybeInjectChaos();

        var order = orders.get(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
                "orderId", orderId,
                "status", order.get("status")));
    }
}
