package com.demo.payment.controller;

import com.demo.payment.chaos.ChaosMonkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final ChaosMonkey chaosMonkey;

    // Simulated payment database
    private final Map<String, Map<String, Object>> payments = new ConcurrentHashMap<>();

    public PaymentController(ChaosMonkey chaosMonkey) {
        this.chaosMonkey = chaosMonkey;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        String userId = (String) request.get("userId");
        double amount = ((Number) request.get("amount")).doubleValue();

        log.info("Processing payment for order {} - user: {}, amount: ${}", orderId, userId, amount);
        chaosMonkey.maybeInjectChaos();

        // Simulate payment processing
        String paymentId = "pay-" + UUID.randomUUID().toString().substring(0, 8);

        // Simulate card validation delay
        try {
            Thread.sleep(50 + (int) (Math.random() * 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Map<String, Object> payment = new ConcurrentHashMap<>();
        payment.put("paymentId", paymentId);
        payment.put("orderId", orderId);
        payment.put("userId", userId);
        payment.put("amount", amount);
        payment.put("status", "COMPLETED");
        payment.put("method", "CREDIT_CARD");
        payment.put("processedAt", Instant.now().toString());

        payments.put(paymentId, payment);

        log.info("Payment {} processed successfully for order {}", paymentId, orderId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentId", paymentId,
                "status", "COMPLETED",
                "amount", amount));
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentId) {
        log.info("Checking payment status: {}", paymentId);
        chaosMonkey.maybeInjectChaos();

        var payment = payments.get(paymentId);
        if (payment == null) {
            log.warn("Payment not found: {}", paymentId);
            return ResponseEntity.notFound().build();
        }

        log.info("Payment {} status: {}", paymentId, payment.get("status"));
        return ResponseEntity.ok(Map.of(
                "paymentId", paymentId,
                "status", payment.get("status"),
                "amount", payment.get("amount")));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable String paymentId) {
        log.info("Processing refund for payment: {}", paymentId);
        chaosMonkey.maybeInjectChaos();

        var payment = payments.get(paymentId);
        if (payment == null) {
            log.warn("Payment not found for refund: {}", paymentId);
            return ResponseEntity.notFound().build();
        }

        if ("REFUNDED".equals(payment.get("status"))) {
            log.warn("Payment {} already refunded", paymentId);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "reason", "Payment already refunded"));
        }

        payment.put("status", "REFUNDED");
        payment.put("refundedAt", Instant.now().toString());

        log.info("Payment {} refunded successfully", paymentId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentId", paymentId,
                "status", "REFUNDED"));
    }
}
