package com.company.loganalyzer.flow.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Represents a complete API call flow graph reconstructed from distributed
 * traces.
 * Each graph represents a single request's journey through the microservices
 * topology.
 */
public record ApiFlowGraph(
        String traceId,
        String rootService,
        String rootEndpoint,
        Instant startTime,
        Duration totalDuration,
        List<ServiceNode> nodes,
        List<FlowEdge> edges,
        FlowStatus status,
        int spanCount,
        List<String> correlatedIncidentIds) {

    public enum FlowStatus {
        SUCCESS,
        PARTIAL_FAILURE,
        FAILURE,
        TIMEOUT
    }

    /**
     * Calculate the critical path duration (longest path through the graph)
     */
    public Duration criticalPathDuration() {
        return edges.stream()
                .map(e -> e.metrics().avgLatency())
                .reduce(Duration.ZERO, Duration::plus);
    }

    /**
     * Check if any node is in failing state
     */
    public boolean hasFailures() {
        return nodes.stream()
                .anyMatch(n -> n.health() == ServiceNode.NodeHealth.FAILING);
    }

    /**
     * Get the bottleneck edge (highest latency)
     */
    public FlowEdge findBottleneck() {
        return edges.stream()
                .max((e1, e2) -> e1.metrics().avgLatency().compareTo(e2.metrics().avgLatency()))
                .orElse(null);
    }
}
