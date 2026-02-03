package com.company.loganalyzer.flow.model;

import java.time.Duration;

/**
 * Represents a service/endpoint node in the API flow graph.
 * Each node corresponds to a unique service + endpoint combination.
 */
public record ServiceNode(
        String id,
        String serviceName,
        String endpoint,
        String method,
        NodeType type,
        NodeHealth health,
        Duration avgLatency,
        double errorRate,
        long requestCount,
        String spanId) {

    public enum NodeType {
        ENTRY, // Entry point (first service in the flow)
        INTERNAL, // Internal microservice
        EXTERNAL, // External API call
        DATABASE, // Database operation
        MESSAGING, // Message queue operation
        CACHE // Cache operation
    }

    public enum NodeHealth {
        HEALTHY, // Error rate < 1%, latency within SLA
        DEGRADED, // Error rate 1-5% or latency above SLA
        FAILING // Error rate > 5% or timeouts
    }

    /**
     * Create a node ID from service name and endpoint
     */
    public static String createId(String serviceName, String endpoint, String method) {
        return String.format("%s:%s:%s", serviceName, method, endpoint)
                .replace("/", "_")
                .replace("{", "")
                .replace("}", "");
    }

    /**
     * Determine node type from service/endpoint characteristics
     */
    public static NodeType inferType(String serviceName, String endpoint) {
        if (endpoint == null)
            return NodeType.INTERNAL;
        String lowerEndpoint = endpoint.toLowerCase();

        if (lowerEndpoint.contains("database") || lowerEndpoint.contains("jdbc")
                || lowerEndpoint.contains("mongo") || lowerEndpoint.contains("redis")) {
            return NodeType.DATABASE;
        }
        if (lowerEndpoint.contains("kafka") || lowerEndpoint.contains("amqp")
                || lowerEndpoint.contains("rabbitmq")) {
            return NodeType.MESSAGING;
        }
        if (lowerEndpoint.contains("cache") || lowerEndpoint.contains("memcached")) {
            return NodeType.CACHE;
        }
        if (serviceName.contains("external") || serviceName.contains("third-party")) {
            return NodeType.EXTERNAL;
        }
        return NodeType.INTERNAL;
    }

    /**
     * Determine health based on error rate and latency thresholds
     */
    public static NodeHealth calculateHealth(double errorRate, Duration avgLatency, Duration slaThreshold) {
        if (errorRate > 0.05 || (slaThreshold != null && avgLatency.compareTo(slaThreshold) > 0)) {
            return NodeHealth.FAILING;
        }
        if (errorRate > 0.01) {
            return NodeHealth.DEGRADED;
        }
        return NodeHealth.HEALTHY;
    }
}
