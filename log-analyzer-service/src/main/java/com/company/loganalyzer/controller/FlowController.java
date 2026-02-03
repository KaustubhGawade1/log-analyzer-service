package com.company.loganalyzer.controller;

import com.company.loganalyzer.flow.model.ApiFlowGraph;
import com.company.loganalyzer.flow.model.DependencyGraph;
import com.company.loganalyzer.flow.service.BottleneckDetector;
import com.company.loganalyzer.flow.service.FlowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for API Flow visualization endpoints.
 * Provides APIs for querying flows, dependencies, and AI analysis.
 */
@RestController
@RequestMapping("/api/flows")
@CrossOrigin(origins = "*")
public class FlowController {

    private final FlowService flowService;
    private final BottleneckDetector bottleneckDetector;

    public FlowController(FlowService flowService, BottleneckDetector bottleneckDetector) {
        this.flowService = flowService;
        this.bottleneckDetector = bottleneckDetector;
    }

    /**
     * Get all services in the system
     */
    @GetMapping("/services")
    public ResponseEntity<List<String>> getServices() {
        return ResponseEntity.ok(flowService.getServices());
    }

    /**
     * Get service-to-service dependency graph
     */
    @GetMapping("/dependencies")
    public ResponseEntity<DependencyGraph> getDependencies(
            @RequestParam(defaultValue = "3600000") long lookbackMs) {
        return ResponseEntity.ok(flowService.getDependencyGraph(lookbackMs));
    }

    /**
     * Get recent traces/flows with optional filtering
     */
    @GetMapping("/traces")
    public ResponseEntity<List<FlowService.FlowSummary>> getTraces(
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "3600000") long lookbackMs) {
        return ResponseEntity.ok(flowService.getRecentFlows(serviceName, limit, lookbackMs));
    }

    /**
     * Get a specific trace as a flow graph
     */
    @GetMapping("/{traceId}")
    public ResponseEntity<ApiFlowGraph> getFlow(@PathVariable String traceId) {
        ApiFlowGraph graph = flowService.getFlow(traceId);
        if (graph == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(graph);
    }

    /**
     * Get flows with detected bottlenecks
     */
    @GetMapping("/bottlenecks")
    public ResponseEntity<List<FlowService.FlowSummary>> getBottleneckFlows(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "3600000") long lookbackMs) {
        return ResponseEntity.ok(flowService.getBottleneckFlows(limit, lookbackMs));
    }

    /**
     * Analyze a flow and get bottleneck details
     */
    @GetMapping("/{traceId}/bottlenecks")
    public ResponseEntity<List<BottleneckDetector.Bottleneck>> getFlowBottlenecks(@PathVariable String traceId) {
        ApiFlowGraph graph = flowService.getFlow(traceId);
        if (graph == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(bottleneckDetector.detectBottlenecks(graph));
    }

    /**
     * Trigger AI explanation for a flow
     */
    @PostMapping("/{traceId}/explain")
    public ResponseEntity<FlowService.FlowAnalysisResult> analyzeFlow(@PathVariable String traceId) {
        FlowService.FlowAnalysisResult result = flowService.analyzeFlow(traceId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Get flow statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<FlowService.FlowStats> getStats(
            @RequestParam(defaultValue = "3600000") long lookbackMs) {
        return ResponseEntity.ok(flowService.getStats(lookbackMs));
    }

    /**
     * Get complete topology graph for visualization
     * Returns nodes and edges formatted for React Flow
     */
    @GetMapping("/topology")
    public ResponseEntity<Map<String, Object>> getTopology(
            @RequestParam(defaultValue = "3600000") long lookbackMs) {
        DependencyGraph deps = flowService.getDependencyGraph(lookbackMs);

        Map<String, Object> result = new HashMap<>();

        // Convert to React Flow format
        List<Map<String, Object>> nodes = deps.services().stream()
                .map(service -> {
                    Map<String, Object> node = new HashMap<>();
                    node.put("id", service);
                    node.put("type", "service");
                    node.put("data", Map.of(
                            "label", service,
                            "fanIn", deps.getFanIn(service),
                            "fanOut", deps.getFanOut(service)));
                    node.put("position", Map.of("x", 0, "y", 0)); // Will be calculated by dagre
                    return node;
                })
                .toList();

        List<Map<String, Object>> edges = deps.dependencies().stream()
                .map(dep -> {
                    Map<String, Object> edge = new HashMap<>();
                    edge.put("id", dep.sourceService() + "->" + dep.targetService());
                    edge.put("source", dep.sourceService());
                    edge.put("target", dep.targetService());
                    edge.put("type", "flow");
                    edge.put("data", Map.of(
                            "requestCount", dep.requestCount(),
                            "avgLatencyMs", dep.avgLatencyMs(),
                            "errorRate", dep.errorRate()));
                    return edge;
                })
                .toList();

        result.put("nodes", nodes);
        result.put("edges", edges);
        result.put("stats", Map.of(
                "serviceCount", deps.services().size(),
                "dependencyCount", deps.dependencies().size(),
                "totalTraces", deps.totalTraces()));

        return ResponseEntity.ok(result);
    }
}
