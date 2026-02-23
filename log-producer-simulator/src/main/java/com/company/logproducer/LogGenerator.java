package com.company.logproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
public class LogGenerator {

    private static final Logger log = LoggerFactory.getLogger(LogGenerator.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();
    private static final String TOPIC = "app-logs";

    private static final String[] SERVICES = {
            "payment-service", "order-service", "user-service",
            "inventory-service", "notification-service", "api-gateway"
    };

    private static final String[] INFO_MESSAGES = {
            "Request processed successfully for user %s",
            "Order %s created with total amount $%.2f",
            "Payment completed for transaction %s",
            "User %s authenticated successfully",
            "Inventory updated for product %s, new quantity: %d",
            "Email notification sent to %s",
            "Cache refreshed for key %s",
            "Health check passed - all dependencies healthy",
            "Connection pool stats: active=%d, idle=%d, waiting=%d",
            "API response time: %dms for endpoint %s"
    };

    private static final String[] WARN_MESSAGES = {
            "Slow database query detected: %dms for table %s",
            "Rate limit approaching for client %s: %d/%d requests",
            "Retry attempt %d for external API call to %s",
            "Memory usage high: %.1f%% of heap utilized",
            "Connection pool exhaustion warning: %d connections in use",
            "Deprecated API endpoint called: %s - migrate to v2",
            "JWT token expires in %d minutes for user %s",
            "Circuit breaker half-open for service %s",
            "Request timeout warning: %dms exceeds threshold",
            "Kafka consumer lag detected: %d messages behind"
    };

    private static final String[] ERROR_MESSAGES = {
            "Database connection timeout after %dms - host: %s:5432",
            "Failed to process payment: Card declined for transaction %s",
            "NullPointerException in OrderService.processOrder() at line %d",
            "503 Service Unavailable from downstream service: %s",
            "Authentication failed: Invalid credentials for user %s",
            "Inventory insufficient: Requested %d but only %d available for product %s",
            "Message queue connection lost: broker %s unreachable",
            "OutOfMemoryError: Java heap space - current usage: %dMB",
            "SSL handshake failed with external API: %s",
            "Deadlock detected in transaction handling thread pool",
            "Redis connection refused: ECONNREFUSED 127.0.0.1:6379",
            "Foreign key constraint violation: Cannot delete user %s with active orders",
            "Request body validation failed: Missing required field '%s'",
            "Circuit breaker OPEN for %s - %d failures in last %d seconds"
    };

    private static final String[] STACK_TRACES = {
            """
                    java.lang.NullPointerException: Cannot invoke method on null object
                        at com.company.payment.PaymentProcessor.processPayment(PaymentProcessor.java:127)
                        at com.company.payment.PaymentService.handlePayment(PaymentService.java:84)
                        at com.company.api.PaymentController.createPayment(PaymentController.java:45)
                        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
                    """,
            """
                    java.sql.SQLException: Connection timed out after 30000ms
                        at com.zaxxer.hikari.pool.HikariProxyConnection.checkException(HikariProxyConnection.java:53)
                        at com.company.repository.OrderRepository.findById(OrderRepository.java:67)
                        at com.company.service.OrderService.getOrder(OrderService.java:112)
                    """,
            """
                    org.springframework.web.client.ResourceAccessException: I/O error on GET request
                        at org.springframework.web.client.RestTemplate.doExecute(RestTemplate.java:785)
                        at com.company.client.InventoryClient.checkStock(InventoryClient.java:34)
                        at com.company.service.OrderService.validateOrder(OrderService.java:89)
                    """,
            """
                    java.util.concurrent.TimeoutException: Request timed out after 5000ms
                        at java.util.concurrent.CompletableFuture.timedGet(CompletableFuture.java:1886)
                        at com.company.service.AsyncPaymentService.processAsync(AsyncPaymentService.java:56)
                    """,
            """
                    io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
                        at io.lettuce.core.RedisClient.connect(RedisClient.java:207)
                        at com.company.cache.RedisCacheManager.getConnection(RedisCacheManager.java:45)
                    """
    };

    public LogGenerator(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 2000)
    public void generateLog() {
        String service = SERVICES[random.nextInt(SERVICES.length)];
        String level = getRandomLevel();
        String message = generateMessage(level);
        String stackTrace = level.equals("ERROR") && random.nextBoolean()
                ? STACK_TRACES[random.nextInt(STACK_TRACES.length)]
                : null;

        LogEvent event = new LogEvent(
                service,
                level,
                message,
                stackTrace,
                Instant.now(),
                generateMetadata(level));

        log.info("Sending log: {}", event);
        kafkaTemplate.send(TOPIC, service, event);
    }

    private String getRandomLevel() {
        int roll = random.nextInt(100);
        if (roll < 60)
            return "INFO"; // 60% INFO
        if (roll < 85)
            return "WARN"; // 25% WARN
        return "ERROR"; // 15% ERROR
    }

    private String generateMessage(String level) {
        // 30% chance to use simple simulated message (keep sample data)
        if (random.nextInt(100) < 30) {
            return "Simulated log message " + random.nextInt(1000);
        }

        return switch (level) {
            case "INFO" -> formatInfoMessage();
            case "WARN" -> formatWarnMessage();
            case "ERROR" -> formatErrorMessage();
            default -> "Unknown log level";
        };
    }

    private String formatInfoMessage() {
        String template = INFO_MESSAGES[random.nextInt(INFO_MESSAGES.length)];
        return String.format(template,
                UUID.randomUUID().toString().substring(0, 8),
                random.nextDouble() * 500,
                random.nextInt(100),
                random.nextInt(50),
                random.nextInt(10),
                "/api/v1/orders");
    }

    private String formatWarnMessage() {
        String template = WARN_MESSAGES[random.nextInt(WARN_MESSAGES.length)];
        return String.format(template,
                random.nextInt(5000) + 1000,
                "users",
                random.nextInt(3) + 1,
                "payment-gateway.example.com",
                75.0 + random.nextDouble() * 20,
                random.nextInt(50) + 10,
                random.nextInt(60) + 5,
                "inventory-service",
                random.nextInt(1000) + 100);
    }

    private String formatErrorMessage() {
        String template = ERROR_MESSAGES[random.nextInt(ERROR_MESSAGES.length)];
        return String.format(template,
                random.nextInt(30000) + 5000,
                "db-primary.internal",
                UUID.randomUUID().toString().substring(0, 12),
                random.nextInt(500) + 100,
                "user-" + random.nextInt(1000),
                random.nextInt(10) + 1,
                random.nextInt(5),
                "product-" + random.nextInt(100),
                "kafka-broker-1",
                random.nextInt(2048) + 512,
                "api.stripe.com",
                random.nextInt(10) + 5,
                random.nextInt(60) + 30,
                "orderId");
    }

    private Map<String, String> generateMetadata(String level) {
        if (level.equals("ERROR")) {
            return Map.of(
                    "traceId", UUID.randomUUID().toString(),
                    "spanId", UUID.randomUUID().toString().substring(0, 16),
                    "userId", "user-" + random.nextInt(1000));
        }
        return Collections.emptyMap();
    }
}
