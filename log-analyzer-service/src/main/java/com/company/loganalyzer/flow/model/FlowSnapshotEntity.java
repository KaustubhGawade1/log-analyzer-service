package com.company.loganalyzer.flow.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for storing flow graph snapshots in PostgreSQL.
 * Stores the full graph as JSON for efficient retrieval.
 */
@Entity
@Table(name = "flow_snapshots")
public class FlowSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trace_id", unique = true, nullable = false, length = 64)
    private String traceId;

    @Column(name = "root_service")
    private String rootService;

    @Column(name = "root_endpoint")
    private String rootEndpoint;

    @Column(name = "graph_json", columnDefinition = "TEXT")
    private String graphJson;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @Column(name = "status", length = 50)
    @Enumerated(EnumType.STRING)
    private ApiFlowGraph.FlowStatus status;

    @Column(name = "node_count")
    private Integer nodeCount;

    @Column(name = "edge_count")
    private Integer edgeCount;

    @Column(name = "has_bottleneck")
    private Boolean hasBottleneck;

    @Column(name = "bottleneck_service")
    private String bottleneckService;

    @Column(name = "ai_explanation", columnDefinition = "TEXT")
    private String aiExplanation;

    @Column(name = "created_at")
    private Instant createdAt;

    public FlowSnapshotEntity() {
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getRootService() {
        return rootService;
    }

    public void setRootService(String rootService) {
        this.rootService = rootService;
    }

    public String getRootEndpoint() {
        return rootEndpoint;
    }

    public void setRootEndpoint(String rootEndpoint) {
        this.rootEndpoint = rootEndpoint;
    }

    public String getGraphJson() {
        return graphJson;
    }

    public void setGraphJson(String graphJson) {
        this.graphJson = graphJson;
    }

    public Long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(Long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public ApiFlowGraph.FlowStatus getStatus() {
        return status;
    }

    public void setStatus(ApiFlowGraph.FlowStatus status) {
        this.status = status;
    }

    public Integer getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
    }

    public Integer getEdgeCount() {
        return edgeCount;
    }

    public void setEdgeCount(Integer edgeCount) {
        this.edgeCount = edgeCount;
    }

    public Boolean getHasBottleneck() {
        return hasBottleneck;
    }

    public void setHasBottleneck(Boolean hasBottleneck) {
        this.hasBottleneck = hasBottleneck;
    }

    public String getBottleneckService() {
        return bottleneckService;
    }

    public void setBottleneckService(String bottleneckService) {
        this.bottleneckService = bottleneckService;
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public void setAiExplanation(String aiExplanation) {
        this.aiExplanation = aiExplanation;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
