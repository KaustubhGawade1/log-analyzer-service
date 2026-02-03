package com.demo.user.controller;

import com.demo.user.chaos.ChaosMonkey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final ChaosMonkey chaosMonkey;

    // Simulated user database
    private final Map<String, Map<String, Object>> users = new ConcurrentHashMap<>();

    public UserController(ChaosMonkey chaosMonkey) {
        this.chaosMonkey = chaosMonkey;
        // Pre-populate some users
        users.put("user-1",
                Map.of("id", "user-1", "name", "John Doe", "email", "john@example.com", "status", "ACTIVE"));
        users.put("user-2",
                Map.of("id", "user-2", "name", "Jane Smith", "email", "jane@example.com", "status", "ACTIVE"));
        users.put("user-3",
                Map.of("id", "user-3", "name", "Bob Wilson", "email", "bob@example.com", "status", "INACTIVE"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        log.info("Getting user: {}", userId);
        chaosMonkey.maybeInjectChaos();

        var user = users.get(userId);
        if (user == null) {
            log.warn("User not found: {}", userId);
            return ResponseEntity.notFound().build();
        }

        log.info("User {} found: {}", userId, user.get("name"));
        return ResponseEntity.ok(user);
    }

    @PostMapping("/{userId}/validate")
    public ResponseEntity<?> validateUser(@PathVariable String userId) {
        log.info("Validating user: {}", userId);
        chaosMonkey.maybeInjectChaos();

        var user = users.get(userId);
        if (user == null) {
            log.warn("Validation failed - user not found: {}", userId);
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "reason", "User not found"));
        }

        if ("INACTIVE".equals(user.get("status"))) {
            log.warn("Validation failed - user inactive: {}", userId);
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "reason", "User is inactive"));
        }

        log.info("User {} validated successfully", userId);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "userId", userId,
                "name", user.get("name")));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request) {
        log.info("Creating new user: {}", request.get("name"));
        chaosMonkey.maybeInjectChaos();

        String userId = "user-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> newUser = new ConcurrentHashMap<>(request);
        newUser.put("id", userId);
        newUser.put("status", "ACTIVE");

        users.put(userId, newUser);

        log.info("User created: {}", userId);
        return ResponseEntity.ok(newUser);
    }
}
