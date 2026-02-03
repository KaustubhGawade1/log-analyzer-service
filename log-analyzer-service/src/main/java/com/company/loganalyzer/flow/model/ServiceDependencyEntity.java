package com.company.loganalyzer.flow.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA Entity for storing aggregated service dependencies in PostgreSQL.
 */
@Entity
@Table(name = "service_dependencies", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "source_service", "target_service", "endpoint_pattern" })
})
public class ServiceDependencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_service", nullable = false)
    private String sourceService;

    @Column(name = "target_service", nullable = false)
    private String targetService;

    @Column(name = "protocol", length = 50)
    private String protocol;

    @Column(name = "endpoint_pattern", length = 500)
    private String endpointPattern;

    @Column(name = "avg_latency_ms")
    private Double avgLatencyMs;

    @Column(name = "p95_latency_ms")
    private Double p95LatencyMs;

    @Column(name = "p99_latency_ms")
    private Double p99LatencyMs;

    @Column(name = "error_rate")
    private Double errorRate;

    @Column(name = "request_count")
    private Long requestCount;

    @Column(name = "failure_count")
    private Long failureCount;

    @Column(name = "first_seen")
    private Instant firstSeen;

    @Column(name = "last_seen")
    private Instant lastSeen;

    public ServiceDependencyEntity() {
        this.requestCount = 0L;
        this.failureCount = 0L;
        this.firstSeen = Instant.now();
        this.lastSeen = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getTargetService() {
        return targetService;
    }

    public void setTargetService(String targetService) {
        this.targetService = targetService;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getEndpointPattern() {
        return endpointPattern;
    }

    public void setEndpointPattern(String endpointPattern) {
        this.endpointPattern = endpointPattern;
    }

    public Double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public void setAvgLatencyMs(Double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
    }

    public Double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(Double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public Double getP99LatencyMs() {
        return p99LatencyMs;
    }

    public void setP99LatencyMs(Double p99LatencyMs) {
        this.p99LatencyMs = p99LatencyMs;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }

    public Long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Long requestCount) {
        this.requestCount = requestCount;
    }

    public Long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Long failureCount) {
        this.failureCount = failureCount;
    }

    public Instant getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Instant firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    /**
     * Update metrics with a new request observation
     */
    public void addRequest(double latencyMs, boolean isError) {
        long oldCount = this.requestCount != null ? this.requestCount : 0;
        double oldAvg = this.avgLatencyMs != null ? this.avgLatencyMs : 0;

        this.requestCount = oldCount + 1;
        this.avgLatencyMs = (oldAvg * oldCount + latencyMs) / this.requestCount;

        if (isError) {
            this.failureCount = (this.failureCount != null ? this.failureCount : 0) + 1;
        }

        this.errorRate = this.requestCount > 0
                ? (double) this.failureCount / this.requestCount
                : 0.0;

        this.lastSeen = Instant.now();
    }
}
