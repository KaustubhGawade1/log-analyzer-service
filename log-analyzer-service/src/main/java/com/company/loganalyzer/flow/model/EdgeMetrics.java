package com.company.loganalyzer.flow.model;

import java.time.Duration;
import java.time.Instant;

/**
 * Performance metrics for an edge in the flow graph.
 * Captures latency percentiles, error rates, and request counts.
 */
public record EdgeMetrics(
        Duration avgLatency,
        Duration p50Latency,
        Duration p95Latency,
        Duration p99Latency,
        double errorRate,
        long requestCount,
        long failureCount,
        long timeoutCount,
        Instant lastUpdated) {

    /**
     * Create metrics from a single span duration
     */
    public static EdgeMetrics fromSingleSpan(Duration duration, boolean isError) {
        return new EdgeMetrics(
                duration,
                duration,
                duration,
                duration,
                isError ? 1.0 : 0.0,
                1,
                isError ? 1 : 0,
                0,
                Instant.now());
    }

    /**
     * Merge two metrics sets (for aggregation)
     */
    public EdgeMetrics merge(EdgeMetrics other) {
        long totalRequests = this.requestCount + other.requestCount;
        long totalFailures = this.failureCount + other.failureCount;

        return new EdgeMetrics(
                weightedAverage(this.avgLatency, this.requestCount, other.avgLatency, other.requestCount),
                this.p50Latency.compareTo(other.p50Latency) > 0 ? this.p50Latency : other.p50Latency,
                this.p95Latency.compareTo(other.p95Latency) > 0 ? this.p95Latency : other.p95Latency,
                this.p99Latency.compareTo(other.p99Latency) > 0 ? this.p99Latency : other.p99Latency,
                totalRequests > 0 ? (double) totalFailures / totalRequests : 0.0,
                totalRequests,
                totalFailures,
                this.timeoutCount + other.timeoutCount,
                this.lastUpdated.isAfter(other.lastUpdated) ? this.lastUpdated : other.lastUpdated);
    }

    private static Duration weightedAverage(Duration d1, long w1, Duration d2, long w2) {
        if (w1 + w2 == 0)
            return Duration.ZERO;
        long totalNanos = (d1.toNanos() * w1 + d2.toNanos() * w2) / (w1 + w2);
        return Duration.ofNanos(totalNanos);
    }

    /**
     * Format latency for display
     */
    public String formatAvgLatency() {
        long millis = avgLatency.toMillis();
        if (millis < 1000) {
            return millis + "ms";
        }
        return String.format("%.2fs", millis / 1000.0);
    }

    /**
     * Format error rate as percentage
     */
    public String formatErrorRate() {
        return String.format("%.1f%%", errorRate * 100);
    }
}
