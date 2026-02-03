package com.company.loganalyzer.flow.service;

import com.company.loganalyzer.flow.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects bottlenecks in API flow graphs.
 * Identifies slow edges, high error rates, and cascading delays.
 */
@Service
public class BottleneckDetector {

    private static final Logger log = LoggerFactory.getLogger(BottleneckDetector.class);

    // Thresholds for bottleneck detection
    private static final double ERROR_RATE_THRESHOLD = 0.05; // 5%
    private static final double LATENCY_MULTIPLIER = 2.0; // 2x average is slow
    private static final long HIGH_LATENCY_MS = 500; // 500ms is slow
    private static final long CRITICAL_LATENCY_MS = 2000; // 2s is critical

    /**
     * Detect all bottlenecks in a flow graph
     */
    public List<Bottleneck> detectBottlenecks(ApiFlowGraph graph) {
        List<Bottleneck> bottlenecks = new ArrayList<>();

        if (graph == null || graph.edges() == null) {
            return bottlenecks;
        }

        // Calculate average latency across all edges
        double avgLatencyMs = graph.edges().stream()
                .mapToLong(e -> e.metrics().avgLatency().toMillis())
                .average()
                .orElse(0);

        for (FlowEdge edge : graph.edges()) {
            EdgeMetrics metrics = edge.metrics();
            long latencyMs = metrics.avgLatency().toMillis();

            // Check for high error rate
            if (metrics.errorRate() > ERROR_RATE_THRESHOLD) {
                bottlenecks.add(new Bottleneck(
                        edge.id(),
                        edge.targetService(),
                        BottleneckType.HIGH_ERROR_RATE,
                        calculateSeverity(metrics.errorRate(), ERROR_RATE_THRESHOLD, 0.5),
                        String.format("Error rate %.1f%% exceeds threshold (%.1f%%)",
                                metrics.errorRate() * 100, ERROR_RATE_THRESHOLD * 100)));
            }

            // Check for high latency (absolute threshold)
            if (latencyMs > CRITICAL_LATENCY_MS) {
                bottlenecks.add(new Bottleneck(
                        edge.id(),
                        edge.targetService(),
                        BottleneckType.CRITICAL_LATENCY,
                        BottleneckSeverity.CRITICAL,
                        String.format("Latency %dms is critically high (>%dms)",
                                latencyMs, CRITICAL_LATENCY_MS)));
            } else if (latencyMs > HIGH_LATENCY_MS) {
                bottlenecks.add(new Bottleneck(
                        edge.id(),
                        edge.targetService(),
                        BottleneckType.HIGH_LATENCY,
                        BottleneckSeverity.HIGH,
                        String.format("Latency %dms exceeds threshold (%dms)",
                                latencyMs, HIGH_LATENCY_MS)));
            }

            // Check for relative slowness (compared to graph average)
            if (avgLatencyMs > 0 && latencyMs > avgLatencyMs * LATENCY_MULTIPLIER) {
                bottlenecks.add(new Bottleneck(
                        edge.id(),
                        edge.targetService(),
                        BottleneckType.RELATIVE_SLOWNESS,
                        BottleneckSeverity.MEDIUM,
                        String.format("Latency %dms is %.1fx slower than average (%.0fms)",
                                latencyMs, latencyMs / avgLatencyMs, avgLatencyMs)));
            }

            // Check for timeouts
            if (metrics.timeoutCount() > 0) {
                bottlenecks.add(new Bottleneck(
                        edge.id(),
                        edge.targetService(),
                        BottleneckType.TIMEOUT,
                        BottleneckSeverity.CRITICAL,
                        String.format("%d timeout(s) detected", metrics.timeoutCount())));
            }
        }

        // Check for fan-out issues (services with many downstream calls)
        for (ServiceNode node : graph.nodes()) {
            long fanOut = graph.edges().stream()
                    .filter(e -> e.sourceNodeId().equals(node.id()))
                    .count();

            if (fanOut > 5) {
                bottlenecks.add(new Bottleneck(
                        node.id(),
                        node.serviceName(),
                        BottleneckType.HIGH_FAN_OUT,
                        fanOut > 10 ? BottleneckSeverity.HIGH : BottleneckSeverity.MEDIUM,
                        String.format("High fan-out: %d downstream calls", fanOut)));
            }
        }

        return bottlenecks;
    }

    /**
     * Find the primary bottleneck in a flow
     */
    public Bottleneck findPrimaryBottleneck(ApiFlowGraph graph) {
        List<Bottleneck> bottlenecks = detectBottlenecks(graph);

        return bottlenecks.stream()
                .max((b1, b2) -> b1.severity().compareTo(b2.severity()))
                .orElse(null);
    }

    private BottleneckSeverity calculateSeverity(double value, double threshold, double criticalThreshold) {
        if (value >= criticalThreshold) {
            return BottleneckSeverity.CRITICAL;
        } else if (value >= threshold * 2) {
            return BottleneckSeverity.HIGH;
        } else if (value >= threshold) {
            return BottleneckSeverity.MEDIUM;
        }
        return BottleneckSeverity.LOW;
    }

    /**
     * Represents a detected bottleneck in the flow
     */
    public record Bottleneck(
            String elementId,
            String serviceName,
            BottleneckType type,
            BottleneckSeverity severity,
            String description) {
    }

    public enum BottleneckType {
        HIGH_ERROR_RATE,
        HIGH_LATENCY,
        CRITICAL_LATENCY,
        RELATIVE_SLOWNESS,
        TIMEOUT,
        HIGH_FAN_OUT,
        CASCADING_FAILURE
    }

    public enum BottleneckSeverity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
