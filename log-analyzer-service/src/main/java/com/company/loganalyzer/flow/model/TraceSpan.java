package com.company.loganalyzer.flow.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Represents a span from Zipkin trace data.
 * Maps to Zipkin's span format for easy deserialization.
 */
public record TraceSpan(
        String traceId,
        String id,
        String parentId,
        String name,
        String kind,
        Instant timestamp,
        Duration duration,
        LocalEndpoint localEndpoint,
        RemoteEndpoint remoteEndpoint,
        Map<String, String> tags,
        boolean hasError) {

    public record LocalEndpoint(
            String serviceName,
            String ipv4,
            int port) {
    }

    public record RemoteEndpoint(
            String serviceName,
            String ipv4,
            int port) {
    }

    /**
     * Check if this is a root span (no parent)
     */
    public boolean isRoot() {
        return parentId == null || parentId.isEmpty();
    }

    /**
     * Get the service name from local endpoint
     */
    public String serviceName() {
        return localEndpoint != null ? localEndpoint.serviceName() : "unknown";
    }

    /**
     * Get the endpoint/operation name
     */
    public String endpoint() {
        return name;
    }

    /**
     * Extract HTTP method from tags if present
     */
    public String httpMethod() {
        if (tags == null)
            return null;
        return tags.getOrDefault("http.method", tags.get("method"));
    }

    /**
     * Extract HTTP path from tags if present
     */
    public String httpPath() {
        if (tags == null)
            return null;
        return tags.getOrDefault("http.path", tags.getOrDefault("http.url", name));
    }

    /**
     * Check if span represents an error
     */
    public boolean isError() {
        if (hasError)
            return true;
        if (tags == null)
            return false;
        String errorTag = tags.get("error");
        String statusCode = tags.get("http.status_code");
        return "true".equalsIgnoreCase(errorTag)
                || (statusCode != null && statusCode.startsWith("5"));
    }

    /**
     * Get span kind (CLIENT, SERVER, PRODUCER, CONSUMER)
     */
    public SpanKind spanKind() {
        if (kind == null)
            return SpanKind.INTERNAL;
        return switch (kind.toUpperCase()) {
            case "CLIENT" -> SpanKind.CLIENT;
            case "SERVER" -> SpanKind.SERVER;
            case "PRODUCER" -> SpanKind.PRODUCER;
            case "CONSUMER" -> SpanKind.CONSUMER;
            default -> SpanKind.INTERNAL;
        };
    }

    public enum SpanKind {
        CLIENT,
        SERVER,
        PRODUCER,
        CONSUMER,
        INTERNAL
    }
}
