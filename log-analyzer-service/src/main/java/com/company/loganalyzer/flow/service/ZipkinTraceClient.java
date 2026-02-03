package com.company.loganalyzer.flow.service;

import com.company.loganalyzer.config.ZipkinConfig;
import com.company.loganalyzer.flow.model.TraceSpan;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * REST client for fetching trace data from Zipkin Query API.
 */
@Service
public class ZipkinTraceClient {

    private static final Logger log = LoggerFactory.getLogger(ZipkinTraceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ZipkinConfig config;

    public ZipkinTraceClient(RestClient zipkinRestClient, ObjectMapper objectMapper, ZipkinConfig config) {
        this.restClient = zipkinRestClient;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    /**
     * Get list of all services reporting traces
     */
    public List<String> getServices() {
        try {
            String response = restClient.get()
                    .uri("/api/v2/services")
                    .retrieve()
                    .body(String.class);

            return objectMapper.readValue(response, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.error("Failed to fetch services from Zipkin", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get span names for a specific service
     */
    public List<String> getSpanNames(String serviceName) {
        try {
            String response = restClient.get()
                    .uri("/api/v2/spans?serviceName={service}", serviceName)
                    .retrieve()
                    .body(String.class);

            return objectMapper.readValue(response, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.error("Failed to fetch spans for service: " + serviceName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get traces for a specific service
     */
    public List<List<TraceSpan>> getTraces(String serviceName, int limit, long lookbackMs) {
        try {
            long endTs = System.currentTimeMillis();

            String uri = String.format(
                    "/api/v2/traces?serviceName=%s&limit=%d&endTs=%d&lookback=%d",
                    serviceName,
                    limit > 0 ? limit : config.getDefaultLimit(),
                    endTs,
                    lookbackMs > 0 ? lookbackMs : config.getDefaultLookbackMs());

            String response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            return parseTraces(response);
        } catch (Exception e) {
            log.error("Failed to fetch traces for service: " + serviceName, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all recent traces (not filtered by service)
     */
    public List<List<TraceSpan>> getRecentTraces(int limit, long lookbackMs) {
        try {
            long endTs = System.currentTimeMillis();

            String uri = String.format(
                    "/api/v2/traces?limit=%d&endTs=%d&lookback=%d",
                    limit > 0 ? limit : config.getDefaultLimit(),
                    endTs,
                    lookbackMs > 0 ? lookbackMs : config.getDefaultLookbackMs());

            String response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);

            return parseTraces(response);
        } catch (Exception e) {
            log.error("Failed to fetch recent traces", e);
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific trace by ID
     */
    public List<TraceSpan> getTrace(String traceId) {
        try {
            String response = restClient.get()
                    .uri("/api/v2/trace/{traceId}", traceId)
                    .retrieve()
                    .body(String.class);

            return parseSpans(objectMapper.readTree(response));
        } catch (Exception e) {
            log.error("Failed to fetch trace: " + traceId, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get dependencies from Zipkin (aggregated service graph)
     */
    public List<Map<String, Object>> getDependencies(long lookbackMs) {
        try {
            long endTs = System.currentTimeMillis();

            String response = restClient.get()
                    .uri("/api/v2/dependencies?endTs={endTs}&lookback={lookback}", endTs, lookbackMs)
                    .retrieve()
                    .body(String.class);

            return objectMapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            log.error("Failed to fetch dependencies from Zipkin", e);
            return Collections.emptyList();
        }
    }

    private List<List<TraceSpan>> parseTraces(String json) throws Exception {
        JsonNode tracesNode = objectMapper.readTree(json);
        List<List<TraceSpan>> traces = new ArrayList<>();

        for (JsonNode traceNode : tracesNode) {
            traces.add(parseSpans(traceNode));
        }

        return traces;
    }

    private List<TraceSpan> parseSpans(JsonNode spansNode) {
        List<TraceSpan> spans = new ArrayList<>();

        for (JsonNode spanNode : spansNode) {
            try {
                spans.add(parseSpan(spanNode));
            } catch (Exception e) {
                log.warn("Failed to parse span: " + spanNode.toString(), e);
            }
        }

        return spans;
    }

    private TraceSpan parseSpan(JsonNode node) {
        String traceId = node.path("traceId").asText();
        String id = node.path("id").asText();
        String parentId = node.path("parentId").asText(null);
        String name = node.path("name").asText();
        String kind = node.path("kind").asText(null);

        // Parse timestamp (microseconds in Zipkin)
        long timestampMicros = node.path("timestamp").asLong(0);
        Instant timestamp = timestampMicros > 0
                ? Instant.ofEpochSecond(0, timestampMicros * 1000)
                : Instant.now();

        // Parse duration (microseconds in Zipkin)
        long durationMicros = node.path("duration").asLong(0);
        Duration duration = Duration.ofNanos(durationMicros * 1000);

        // Parse endpoints
        TraceSpan.LocalEndpoint localEndpoint = parseLocalEndpoint(node.path("localEndpoint"));
        TraceSpan.RemoteEndpoint remoteEndpoint = parseRemoteEndpoint(node.path("remoteEndpoint"));

        // Parse tags
        Map<String, String> tags = new HashMap<>();
        JsonNode tagsNode = node.path("tags");
        if (tagsNode.isObject()) {
            tagsNode.fields().forEachRemaining(entry -> tags.put(entry.getKey(), entry.getValue().asText()));
        }

        // Check for error
        boolean hasError = tags.containsKey("error")
                || (tags.containsKey("http.status_code") && tags.get("http.status_code").startsWith("5"));

        return new TraceSpan(
                traceId, id, parentId, name, kind,
                timestamp, duration,
                localEndpoint, remoteEndpoint,
                tags, hasError);
    }

    private TraceSpan.LocalEndpoint parseLocalEndpoint(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new TraceSpan.LocalEndpoint(
                node.path("serviceName").asText("unknown"),
                node.path("ipv4").asText(null),
                node.path("port").asInt(0));
    }

    private TraceSpan.RemoteEndpoint parseRemoteEndpoint(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return new TraceSpan.RemoteEndpoint(
                node.path("serviceName").asText(null),
                node.path("ipv4").asText(null),
                node.path("port").asInt(0));
    }
}
