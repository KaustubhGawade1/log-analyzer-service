package com.demo.gateway.controller;

import com.demo.gateway.chaos.ChaosMonkey;
import com.demo.gateway.kafka.KafkaLogForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final RestTemplate restTemplate;
    private final ChaosMonkey chaosMonkey;
    private final KafkaLogForwarder forwarder;

    @Value("${services.user-service}")
    private String userServiceUrl;

    @Value("${services.order-service}")
    private String orderServiceUrl;

    public GatewayController(RestTemplate restTemplate, ChaosMonkey chaosMonkey, KafkaLogForwarder forwarder) {
        this.restTemplate = restTemplate;
        this.chaosMonkey = chaosMonkey;
        this.forwarder = forwarder;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        log.info("Health check requested");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "api-gateway"));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        log.info("Fetching user: {}", userId);
        forwarder.info("Fetching user: " + userId);
        chaosMonkey.maybeInjectChaos();

        try {
            String url = userServiceUrl + "/users/" + userId;
            var response = restTemplate.getForEntity(url, Map.class);

            log.info("User {} retrieved successfully", userId);
            forwarder.info("User " + userId + " retrieved successfully");
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch user: {}", userId, e);
            forwarder.error("Failed to fetch user: " + userId, e);
            throw e;
        }
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        String userId = (String) orderRequest.get("userId");
        log.info("Creating order for user: {}", userId);
        forwarder.info("Creating order for user: " + userId);
        chaosMonkey.maybeInjectChaos();

        try {
            // First validate user
            String validateUrl = userServiceUrl + "/users/" + userId + "/validate";
            restTemplate.postForEntity(validateUrl, null, Map.class);
            log.info("User {} validated", userId);
            forwarder.info("User " + userId + " validated");

            // Then create order
            String orderUrl = orderServiceUrl + "/orders";
            var response = restTemplate.postForEntity(orderUrl, orderRequest, Map.class);

            log.info("Order created successfully for user: {}", userId);
            forwarder.info("Order created successfully for user: " + userId);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to create order for user: {}", userId, e);
            forwarder.error("Failed to create order for user: " + userId, e);
            throw e;
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        log.info("Fetching order: {}", orderId);
        chaosMonkey.maybeInjectChaos();

        String url = orderServiceUrl + "/orders/" + orderId;
        var response = restTemplate.getForEntity(url, Map.class);

        log.info("Order {} retrieved", orderId);
        return ResponseEntity.ok(response.getBody());
    }
}
