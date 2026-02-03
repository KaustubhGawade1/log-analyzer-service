package com.company.loganalyzer.flow.model;

import java.util.List;

/**
 * Represents an edge (call) between two service nodes in the flow graph.
 * Contains latency, error rate, and request count metrics.
 */
public record FlowEdge(
        String id,
        String sourceNodeId,
        String targetNodeId,
        String sourceService,
        String targetService,
        EdgeMetrics metrics,
        EdgeStatus status,
        String protocol,
        List<String> sampleTraceIds) {

    public enum EdgeStatus {
        NORMAL, // Latency and error rate within thresholds
        SLOW, // Latency above P95 threshold
        FAILING, // Error rate above threshold
        TIMEOUT, // Timeouts detected
        RETRYING // Retries detected
    }

    /**
     * Create an edge ID from source and target
     */
    public static String createId(String sourceNodeId, String targetNodeId) {
        return sourceNodeId + "->" + targetNodeId;
    }

    /**
     * Determine edge status based on metrics
     */
    public static EdgeStatus calculateStatus(EdgeMetrics metrics, EdgeThresholds thresholds) {
        if (metrics.timeoutCount() > 0) {
            return EdgeStatus.TIMEOUT;
        }
        if (metrics.errorRate() > thresholds.errorRateThreshold()) {
            return EdgeStatus.FAILING;
        }
        if (metrics.p95Latency().toMillis() > thresholds.p95LatencyThresholdMs()) {
            return EdgeStatus.SLOW;
        }
        return EdgeStatus.NORMAL;
    }

    /**
     * Configuration for edge status thresholds
     */
    public record EdgeThresholds(
            double errorRateThreshold,
            long p95LatencyThresholdMs,
            long timeoutThresholdMs) {
        public static EdgeThresholds defaults() {
            return new EdgeThresholds(0.05, 500, 5000);
        }
    }
}
