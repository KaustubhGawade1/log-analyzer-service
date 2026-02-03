package com.company.loganalyzer.flow.service;

import com.company.loganalyzer.ai.AiFlowExplanationService;
import com.company.loganalyzer.flow.model.*;
import com.company.loganalyzer.flow.repository.FlowSnapshotRepository;
import com.company.loganalyzer.flow.repository.ServiceDependencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main service for API flow operations.
 * Orchestrates trace fetching, graph building, analysis, and persistence.
 */
@Service
public class FlowService {

    private static final Logger log = LoggerFactory.getLogger(FlowService.class);

    private final ZipkinTraceClient zipkinClient;
    private final FlowGraphBuilder graphBuilder;
    private final BottleneckDetector bottleneckDetector;
    private final AiFlowExplanationService aiExplanationService;
    private final FlowSnapshotRepository snapshotRepository;
    private final ServiceDependencyRepository dependencyRepository;
    private final ObjectMapper objectMapper;

    // In-memory cache for recent flows (as per user preference)
    private final Map<String, ApiFlowGraph> flowCache = new ConcurrentHashMap<>();
    private static final int CACHE_MAX_SIZE = 1000;

    public FlowService(
            ZipkinTraceClient zipkinClient,
            FlowGraphBuilder graphBuilder,
            BottleneckDetector bottleneckDetector,
            AiFlowExplanationService aiExplanationService,
            FlowSnapshotRepository snapshotRepository,
            ServiceDependencyRepository dependencyRepository,
            ObjectMapper objectMapper) {
        this.zipkinClient = zipkinClient;
        this.graphBuilder = graphBuilder;
        this.bottleneckDetector = bottleneckDetector;
        this.aiExplanationService = aiExplanationService;
        this.snapshotRepository = snapshotRepository;
        this.dependencyRepository = dependencyRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all services from Zipkin
     */
    public List<String> getServices() {
        return zipkinClient.getServices();
    }

    /**
     * Get the service dependency graph
     */
    public DependencyGraph getDependencyGraph(long lookbackMs) {
        List<List<TraceSpan>> traces = zipkinClient.getRecentTraces(100, lookbackMs);
        return graphBuilder.buildDependencyGraph(traces);
    }

    /**
     * Get a specific flow graph by trace ID
     */
    public ApiFlowGraph getFlow(String traceId) {
        // Check cache first
        if (flowCache.containsKey(traceId)) {
            return flowCache.get(traceId);
        }

        // Check database
        Optional<FlowSnapshotEntity> cached = snapshotRepository.findByTraceId(traceId);
        if (cached.isPresent()) {
            try {
                ApiFlowGraph graph = objectMapper.readValue(cached.get().getGraphJson(), ApiFlowGraph.class);
                flowCache.put(traceId, graph);
                return graph;
            } catch (Exception e) {
                log.warn("Failed to deserialize cached flow: " + traceId, e);
            }
        }

        // Fetch from Zipkin
        List<TraceSpan> spans = zipkinClient.getTrace(traceId);
        if (spans.isEmpty()) {
            return null;
        }

        ApiFlowGraph graph = graphBuilder.buildFromTrace(spans);

        // Cache the result
        cacheFlow(traceId, graph);

        return graph;
    }

    /**
     * Get recent flows with optional filtering
     */
    public List<FlowSummary> getRecentFlows(String serviceName, int limit, long lookbackMs) {
        List<FlowSummary> summaries = new ArrayList<>();

        List<List<TraceSpan>> traces;
        if (serviceName != null && !serviceName.isEmpty()) {
            traces = zipkinClient.getTraces(serviceName, limit, lookbackMs);
        } else {
            traces = zipkinClient.getRecentTraces(limit, lookbackMs);
        }

        for (List<TraceSpan> trace : traces) {
            if (!trace.isEmpty()) {
                ApiFlowGraph graph = graphBuilder.buildFromTrace(trace);
                if (graph != null) {
                    BottleneckDetector.Bottleneck bottleneck = bottleneckDetector.findPrimaryBottleneck(graph);
                    summaries.add(new FlowSummary(
                            graph.traceId(),
                            graph.rootService(),
                            graph.rootEndpoint(),
                            graph.startTime(),
                            graph.totalDuration().toMillis(),
                            graph.status(),
                            graph.nodes().size(),
                            bottleneck != null,
                            bottleneck != null ? bottleneck.serviceName() : null));
                }
            }
        }

        return summaries;
    }

    /**
     * Get flows with detected bottlenecks
     */
    public List<FlowSummary> getBottleneckFlows(int limit, long lookbackMs) {
        List<FlowSummary> allFlows = getRecentFlows(null, limit * 2, lookbackMs);

        return allFlows.stream()
                .filter(FlowSummary::hasBottleneck)
                .limit(limit)
                .toList();
    }

    /**
     * Analyze a flow and generate AI explanation
     */
    @Transactional
    public FlowAnalysisResult analyzeFlow(String traceId) {
        ApiFlowGraph graph = getFlow(traceId);
        if (graph == null) {
            return null;
        }

        List<BottleneckDetector.Bottleneck> bottlenecks = bottleneckDetector.detectBottlenecks(graph);
        AiFlowExplanationService.FlowExplanation explanation = aiExplanationService.explainFlow(graph);

        // Update database with AI explanation
        snapshotRepository.findByTraceId(traceId).ifPresent(entity -> {
            entity.setAiExplanation(explanation.summary());
            entity.setBottleneckService(explanation.bottleneckService());
            entity.setHasBottleneck(!bottlenecks.isEmpty());
            snapshotRepository.save(entity);
        });

        return new FlowAnalysisResult(graph, bottlenecks, explanation);
    }

    /**
     * Get flow statistics
     */
    public FlowStats getStats(long lookbackMs) {
        Instant since = Instant.now().minus(lookbackMs, ChronoUnit.MILLIS);

        long totalFlows = snapshotRepository.countRecentFlows(since);
        List<Object[]> statusCounts = snapshotRepository.getStatusCounts(since);

        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] row : statusCounts) {
            statusMap.put(row[0].toString(), ((Number) row[1]).longValue());
        }

        List<String> services = zipkinClient.getServices();

        return new FlowStats(
                totalFlows,
                statusMap.getOrDefault("SUCCESS", 0L),
                statusMap.getOrDefault("PARTIAL_FAILURE", 0L) + statusMap.getOrDefault("FAILURE", 0L),
                services.size(),
                services);
    }

    private void cacheFlow(String traceId, ApiFlowGraph graph) {
        // Simple cache eviction
        if (flowCache.size() >= CACHE_MAX_SIZE) {
            String oldestKey = flowCache.keySet().iterator().next();
            flowCache.remove(oldestKey);
        }
        flowCache.put(traceId, graph);

        // Persist to database
        try {
            FlowSnapshotEntity entity = new FlowSnapshotEntity();
            entity.setTraceId(traceId);
            entity.setRootService(graph.rootService());
            entity.setRootEndpoint(graph.rootEndpoint());
            entity.setGraphJson(objectMapper.writeValueAsString(graph));
            entity.setTotalDurationMs(graph.totalDuration().toMillis());
            entity.setStatus(graph.status());
            entity.setNodeCount(graph.nodes().size());
            entity.setEdgeCount(graph.edges().size());

            snapshotRepository.save(entity);
        } catch (Exception e) {
            log.warn("Failed to persist flow snapshot: " + traceId, e);
        }
    }

    /**
     * Summary of a flow for list view
     */
    public record FlowSummary(
            String traceId,
            String rootService,
            String rootEndpoint,
            Instant startTime,
            long durationMs,
            ApiFlowGraph.FlowStatus status,
            int nodeCount,
            boolean hasBottleneck,
            String bottleneckService) {
    }

    /**
     * Complete analysis result including graph, bottlenecks, and AI explanation
     */
    public record FlowAnalysisResult(
            ApiFlowGraph graph,
            List<BottleneckDetector.Bottleneck> bottlenecks,
            AiFlowExplanationService.FlowExplanation explanation) {
    }

    /**
     * Flow statistics
     */
    public record FlowStats(
            long totalFlows,
            long successfulFlows,
            long failedFlows,
            int serviceCount,
            List<String> services) {
    }
}
