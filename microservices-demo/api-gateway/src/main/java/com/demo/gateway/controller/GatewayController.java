package com.demo.gateway.controller;

import com.demo.gateway.chaos.ChaosMonkey;
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

    @Value("${services.user-service}")
    private String userServiceUrl;

    @Value("${services.order-service}")
    private String orderServiceUrl;

    public GatewayController(RestTemplate restTemplate, ChaosMonkey chaosMonkey) {
        this.restTemplate = restTemplate;
        this.chaosMonkey = chaosMonkey;
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
        chaosMonkey.maybeInjectChaos();

        String url = userServiceUrl + "/users/" + userId;
        var response = restTemplate.getForEntity(url, Map.class);

        log.info("User {} retrieved successfully", userId);
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> orderRequest) {
        String userId = (String) orderRequest.get("userId");
        log.info("Creating order for user: {}", userId);
        chaosMonkey.maybeInjectChaos();

        // First validate user
        String validateUrl = userServiceUrl + "/users/" + userId + "/validate";
        restTemplate.postForEntity(validateUrl, null, Map.class);
        log.info("User {} validated", userId);

        // Then create order
        String orderUrl = orderServiceUrl + "/orders";
        var response = restTemplate.postForEntity(orderUrl, orderRequest, Map.class);

        log.info("Order created successfully for user: {}", userId);
        return ResponseEntity.ok(response.getBody());
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
