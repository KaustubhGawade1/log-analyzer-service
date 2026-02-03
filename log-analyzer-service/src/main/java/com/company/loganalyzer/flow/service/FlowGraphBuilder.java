package com.company.loganalyzer.flow.service;

import com.company.loganalyzer.flow.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds API flow graphs from Zipkin trace spans.
 * Reconstructs the call graph showing how a request flows through services.
 */
@Service
public class FlowGraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(FlowGraphBuilder.class);
    private static final Duration DEFAULT_SLA = Duration.ofMillis(500);

    /**
     * Build a complete API flow graph from trace spans
     */
    public ApiFlowGraph buildFromTrace(List<TraceSpan> spans) {
        if (spans == null || spans.isEmpty()) {
            return null;
        }

        String traceId = spans.get(0).traceId();

        // Build span lookup maps
        Map<String, TraceSpan> spanById = spans.stream()
                .collect(Collectors.toMap(TraceSpan::id, s -> s, (s1, s2) -> s1));

        Map<String, List<TraceSpan>> spansByParent = spans.stream()
                .filter(s -> s.parentId() != null)
                .collect(Collectors.groupingBy(TraceSpan::parentId));

        // Find root span
        TraceSpan rootSpan = spans.stream()
                .filter(TraceSpan::isRoot)
                .findFirst()
                .orElse(spans.get(0));

        // Build nodes (unique service+endpoint combinations)
        Map<String, ServiceNode> nodeMap = new HashMap<>();
        for (TraceSpan span : spans) {
            String nodeId = createNodeId(span);
            if (!nodeMap.containsKey(nodeId)) {
                nodeMap.put(nodeId, createNode(span, nodeId, span.equals(rootSpan)));
            }
        }

        // Build edges (parent-child relationships)
        List<FlowEdge> edges = new ArrayList<>();
        Set<String> edgeIds = new HashSet<>();

        for (TraceSpan span : spans) {
            if (span.parentId() != null && spanById.containsKey(span.parentId())) {
                TraceSpan parent = spanById.get(span.parentId());
                String sourceNodeId = createNodeId(parent);
                String targetNodeId = createNodeId(span);
                String edgeId = FlowEdge.createId(sourceNodeId, targetNodeId);

                if (!edgeIds.contains(edgeId) && !sourceNodeId.equals(targetNodeId)) {
                    edges.add(createEdge(parent, span, sourceNodeId, targetNodeId));
                    edgeIds.add(edgeId);
                }
            }
        }

        // Calculate total duration
        Instant startTime = rootSpan.timestamp();
        Duration totalDuration = rootSpan.duration();

        // Determine overall status
        ApiFlowGraph.FlowStatus status = determineStatus(spans);

        return new ApiFlowGraph(
                traceId,
                rootSpan.serviceName(),
                rootSpan.endpoint(),
                startTime,
                totalDuration,
                new ArrayList<>(nodeMap.values()),
                edges,
                status,
                spans.size(),
                Collections.emptyList());
    }

    /**
     * Build a dependency graph from multiple traces
     */
    public DependencyGraph buildDependencyGraph(List<List<TraceSpan>> traces) {
        Map<String, DependencyGraph.ServiceDependency> depMap = new HashMap<>();
        Set<String> allServices = new HashSet<>();

        for (List<TraceSpan> trace : traces) {
            Map<String, TraceSpan> spanById = trace.stream()
                    .collect(Collectors.toMap(TraceSpan::id, s -> s, (s1, s2) -> s1));

            for (TraceSpan span : trace) {
                allServices.add(span.serviceName());

                if (span.parentId() != null && spanById.containsKey(span.parentId())) {
                    TraceSpan parent = spanById.get(span.parentId());
                    String parentService = parent.serviceName();
                    String childService = span.serviceName();

                    // Only track cross-service calls
                    if (!parentService.equals(childService)) {
                        String key = parentService + "->" + childService;

                        DependencyGraph.ServiceDependency existing = depMap.get(key);
                        if (existing == null) {
                            depMap.put(key, new DependencyGraph.ServiceDependency(
                                    parentService,
                                    childService,
                                    "HTTP",
                                    1,
                                    span.duration().toMillis(),
                                    span.duration().toMillis(),
                                    span.isError() ? 1.0 : 0.0,
                                    span.timestamp(),
                                    span.timestamp()));
                        } else {
                            // Update aggregated metrics
                            long newCount = existing.requestCount() + 1;
                            double newAvg = (existing.avgLatencyMs() * existing.requestCount()
                                    + span.duration().toMillis()) / newCount;
                            double newP95 = Math.max(existing.p95LatencyMs(), span.duration().toMillis());
                            long errors = span.isError() ? 1 : 0;
                            double newErrorRate = (existing.errorRate() * existing.requestCount() + errors) / newCount;

                            depMap.put(key, new DependencyGraph.ServiceDependency(
                                    parentService,
                                    childService,
                                    "HTTP",
                                    newCount,
                                    newAvg,
                                    newP95,
                                    newErrorRate,
                                    existing.firstSeen(),
                                    span.timestamp()));
                        }
                    }
                }
            }
        }

        return new DependencyGraph(
                new ArrayList<>(depMap.values()),
                new ArrayList<>(allServices),
                Instant.now(),
                traces.size());
    }

    private String createNodeId(TraceSpan span) {
        return ServiceNode.createId(span.serviceName(), span.endpoint(), span.httpMethod());
    }

    private ServiceNode createNode(TraceSpan span, String nodeId, boolean isRoot) {
        ServiceNode.NodeType type = isRoot
                ? ServiceNode.NodeType.ENTRY
                : ServiceNode.inferType(span.serviceName(), span.endpoint());

        ServiceNode.NodeHealth health = ServiceNode.calculateHealth(
                span.isError() ? 1.0 : 0.0,
                span.duration(),
                DEFAULT_SLA);

        return new ServiceNode(
                nodeId,
                span.serviceName(),
                span.httpPath() != null ? span.httpPath() : span.endpoint(),
                span.httpMethod(),
                type,
                health,
                span.duration(),
                span.isError() ? 1.0 : 0.0,
                1,
                span.id());
    }

    private FlowEdge createEdge(TraceSpan parent, TraceSpan child, String sourceId, String targetId) {
        EdgeMetrics metrics = EdgeMetrics.fromSingleSpan(child.duration(), child.isError());
        FlowEdge.EdgeStatus status = FlowEdge.calculateStatus(metrics, FlowEdge.EdgeThresholds.defaults());

        return new FlowEdge(
                FlowEdge.createId(sourceId, targetId),
                sourceId,
                targetId,
                parent.serviceName(),
                child.serviceName(),
                metrics,
                status,
                "HTTP",
                List.of(child.traceId()));
    }

    private ApiFlowGraph.FlowStatus determineStatus(List<TraceSpan> spans) {
        long errorCount = spans.stream().filter(TraceSpan::isError).count();

        if (errorCount == 0) {
            return ApiFlowGraph.FlowStatus.SUCCESS;
        } else if (errorCount == spans.size()) {
            return ApiFlowGraph.FlowStatus.FAILURE;
        } else {
            return ApiFlowGraph.FlowStatus.PARTIAL_FAILURE;
        }
    }
}
