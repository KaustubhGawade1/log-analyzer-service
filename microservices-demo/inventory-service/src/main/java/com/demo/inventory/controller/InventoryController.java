package com.demo.inventory.controller;

import com.demo.inventory.chaos.ChaosMonkey;
import com.demo.inventory.kafka.KafkaLogForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    private final ChaosMonkey chaosMonkey;
    private final KafkaLogForwarder forwarder;

    // Simulated inventory database
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> reservations = new ConcurrentHashMap<>();

    public InventoryController(ChaosMonkey chaosMonkey, KafkaLogForwarder forwarder) {
        this.chaosMonkey = chaosMonkey;
        this.forwarder = forwarder;
        // Pre-populate inventory
        inventory.put("product-1", 100);
        inventory.put("product-2", 50);
        inventory.put("product-3", 200);
        inventory.put("product-4", 0); // Out of stock
    }

    @GetMapping("/{productId}")
    public ResponseEntity<?> checkStock(@PathVariable String productId) {
        log.info("Checking stock for product: {}", productId);
        forwarder.info("Checking stock for product: " + productId);
        chaosMonkey.maybeInjectChaos();

        Integer stock = inventory.get(productId);
        if (stock == null) {
            log.warn("Product not found: {}", productId);
            forwarder.warn("Product not found: " + productId);
            return ResponseEntity.notFound().build();
        }

        log.info("Stock for {}: {}", productId, stock);
        forwarder.info("Stock for " + productId + ": " + stock);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "availableQuantity", stock,
                "inStock", stock > 0));
    }

    @PostMapping("/reserve")
    public ResponseEntity<?> reserveInventory(@RequestBody Map<String, Object> request) {
        String productId = (String) request.get("productId");
        int quantity = (int) request.getOrDefault("quantity", 1);
        String orderId = (String) request.get("orderId");

        log.info("Reserving {} units of {} for order {}", quantity, productId, orderId);
        forwarder.info("Reserving " + quantity + " units of " + productId + " for order " + orderId);
        chaosMonkey.maybeInjectChaos();

        Integer stock = inventory.get(productId);
        if (stock == null) {
            log.error("Product not found: {}", productId);
            forwarder.error("Product not found: " + productId);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "reason", "Product not found"));
        }

        if (stock < quantity) {
            log.warn("Insufficient stock for {}: requested {}, available {}", productId, quantity, stock);
            forwarder.warn("Insufficient stock for " + productId + ": requested " + quantity + ", available " + stock);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "reason", "Insufficient stock",
                    "available", stock,
                    "requested", quantity));
        }

        // Reserve the inventory
        inventory.put(productId, stock - quantity);
        String reservationId = "rsv-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> reservation = new ConcurrentHashMap<>();
        reservation.put("reservationId", reservationId);
        reservation.put("productId", productId);
        reservation.put("quantity", quantity);
        reservation.put("orderId", orderId);
        reservation.put("status", "RESERVED");

        reservations.put(reservationId, reservation);

        log.info("Reservation {} created for order {}", reservationId, orderId);
        forwarder.info("Reservation " + reservationId + " created for order " + orderId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "reservationId", reservationId,
                "productId", productId,
                "quantity", quantity));
    }

    @PostMapping("/release")
    public ResponseEntity<?> releaseInventory(@RequestBody Map<String, Object> request) {
        String reservationId = (String) request.get("reservationId");

        log.info("Releasing reservation: {}", reservationId);
        chaosMonkey.maybeInjectChaos();

        var reservation = reservations.get(reservationId);
        if (reservation == null) {
            log.warn("Reservation not found: {}", reservationId);
            return ResponseEntity.notFound().build();
        }

        String productId = (String) reservation.get("productId");
        int quantity = (int) reservation.get("quantity");

        // Return inventory
        inventory.merge(productId, quantity, Integer::sum);
        reservation.put("status", "RELEASED");

        log.info("Reservation {} released, {} units returned to {}", reservationId, quantity, productId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "reservationId", reservationId,
                "status", "RELEASED"));
    }
}
