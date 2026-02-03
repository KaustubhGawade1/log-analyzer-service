package com.company.loganalyzer.flow.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents the service-to-service dependency topology.
 * Aggregated view of all flows showing which services call which other
 * services.
 */
public record DependencyGraph(
        List<ServiceDependency> dependencies,
        List<String> services,
        Instant lastUpdated,
        long totalTraces) {

    /**
     * Represents a single dependency between two services
     */
    public record ServiceDependency(
            String sourceService,
            String targetService,
            String protocol,
            long requestCount,
            double avgLatencyMs,
            double p95LatencyMs,
            double errorRate,
            Instant firstSeen,
            Instant lastSeen) {
    }

    /**
     * Get all services that a given service depends on (outgoing edges)
     */
    public List<String> getDownstreamServices(String serviceName) {
        return dependencies.stream()
                .filter(d -> d.sourceService().equals(serviceName))
                .map(ServiceDependency::targetService)
                .distinct()
                .toList();
    }

    /**
     * Get all services that depend on a given service (incoming edges)
     */
    public List<String> getUpstreamServices(String serviceName) {
        return dependencies.stream()
                .filter(d -> d.targetService().equals(serviceName))
                .map(ServiceDependency::sourceService)
                .distinct()
                .toList();
    }

    /**
     * Get the fan-out count for a service (number of downstream dependencies)
     */
    public int getFanOut(String serviceName) {
        return getDownstreamServices(serviceName).size();
    }

    /**
     * Get the fan-in count for a service (number of upstream callers)
     */
    public int getFanIn(String serviceName) {
        return getUpstreamServices(serviceName).size();
    }
}
