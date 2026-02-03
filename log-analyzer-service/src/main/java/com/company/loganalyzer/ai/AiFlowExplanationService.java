package com.company.loganalyzer.ai;

import com.company.loganalyzer.flow.model.ApiFlowGraph;
import com.company.loganalyzer.flow.model.FlowEdge;
import com.company.loganalyzer.flow.model.ServiceNode;
import com.company.loganalyzer.flow.service.BottleneckDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI-powered service for generating natural language explanations of API flows.
 * Uses Spring AI to analyze flow patterns and provide actionable insights.
 */
@Service
public class AiFlowExplanationService {

    private static final Logger log = LoggerFactory.getLogger(AiFlowExplanationService.class);

    private final ChatClient chatClient;
    private final BottleneckDetector bottleneckDetector;

    public AiFlowExplanationService(ChatClient.Builder chatClientBuilder, BottleneckDetector bottleneckDetector) {
        this.chatClient = chatClientBuilder.build();
        this.bottleneckDetector = bottleneckDetector;
    }

    /**
     * Generate an AI explanation for a flow graph
     */
    public FlowExplanation explainFlow(ApiFlowGraph graph) {
        if (graph == null) {
            return new FlowExplanation(
                    "No flow data available",
                    null,
                    null,
                    List.of(),
                    "low");
        }

        try {
            String flowDescription = buildFlowDescription(graph);
            List<BottleneckDetector.Bottleneck> bottlenecks = bottleneckDetector.detectBottlenecks(graph);

            String systemPrompt = """
                    You are a Senior Site Reliability Engineer analyzing API call flows.
                    Given the following service call graph with latency and error metrics,
                    provide a concise analysis explaining:
                    1. A brief summary of the flow behavior
                    2. The primary bottleneck service (if any)
                    3. The most likely root cause
                    4. Specific, actionable recommendations to improve performance

                    Be specific and technical. Reference actual service names and metrics.

                    Output your response as valid JSON matching this exact structure:
                    {
                        "summary": "Brief 1-2 sentence summary of the flow behavior",
                        "bottleneckService": "Name of the bottleneck service or null",
                        "rootCause": "Most likely root cause explanation",
                        "recommendations": ["Specific action 1", "Specific action 2", "..."],
                        "estimatedImpact": "high|medium|low"
                    }

                    Only output the JSON, no markdown formatting or additional text.
                    """;

            String userPrompt = String.format("""
                    ## API Flow Analysis Request

                    ### Flow Overview
                    %s

                    ### Detected Bottlenecks
                    %s

                    Please analyze this flow and provide recommendations.
                    """,
                    flowDescription,
                    formatBottlenecks(bottlenecks));

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            return parseResponse(response);

        } catch (Exception e) {
            log.error("Failed to generate AI explanation for flow: " + graph.traceId(), e);
            return generateFallbackExplanation(graph);
        }
    }

    private String buildFlowDescription(ApiFlowGraph graph) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("- **Trace ID**: %s\n", graph.traceId()));
        sb.append(String.format("- **Root Service**: %s\n", graph.rootService()));
        sb.append(String.format("- **Root Endpoint**: %s\n", graph.rootEndpoint()));
        sb.append(String.format("- **Total Duration**: %dms\n", graph.totalDuration().toMillis()));
        sb.append(String.format("- **Status**: %s\n", graph.status()));
        sb.append(String.format("- **Nodes**: %d services\n", graph.nodes().size()));
        sb.append(String.format("- **Edges**: %d calls\n", graph.edges().size()));

        sb.append("\n### Service Nodes\n");
        for (ServiceNode node : graph.nodes()) {
            sb.append(String.format("- **%s** (%s): %s, latency=%dms, errorRate=%.1f%%\n",
                    node.serviceName(),
                    node.type(),
                    node.health(),
                    node.avgLatency().toMillis(),
                    node.errorRate() * 100));
        }

        sb.append("\n### Call Edges\n");
        for (FlowEdge edge : graph.edges()) {
            sb.append(String.format("- %s → %s: latency=%dms, p95=%dms, errorRate=%.1f%%, status=%s\n",
                    edge.sourceService(),
                    edge.targetService(),
                    edge.metrics().avgLatency().toMillis(),
                    edge.metrics().p95Latency().toMillis(),
                    edge.metrics().errorRate() * 100,
                    edge.status()));
        }

        return sb.toString();
    }

    private String formatBottlenecks(List<BottleneckDetector.Bottleneck> bottlenecks) {
        if (bottlenecks.isEmpty()) {
            return "No significant bottlenecks detected.";
        }

        return bottlenecks.stream()
                .map(b -> String.format("- **%s** (%s, %s): %s",
                        b.serviceName(),
                        b.type(),
                        b.severity(),
                        b.description()))
                .collect(Collectors.joining("\n"));
    }

    private FlowExplanation parseResponse(String response) {
        try {
            // Clean up response if it has markdown formatting
            String cleaned = response.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            }
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            // Parse using Jackson
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(cleaned, FlowExplanation.class);

        } catch (Exception e) {
            log.warn("Failed to parse AI response: " + response, e);
            return new FlowExplanation(
                    response,
                    null,
                    null,
                    List.of(),
                    "medium");
        }
    }

    private FlowExplanation generateFallbackExplanation(ApiFlowGraph graph) {
        BottleneckDetector.Bottleneck primary = bottleneckDetector.findPrimaryBottleneck(graph);

        String summary = String.format(
                "Flow from %s completed in %dms with %d service calls. Status: %s",
                graph.rootService(),
                graph.totalDuration().toMillis(),
                graph.nodes().size(),
                graph.status());

        String bottleneckService = primary != null ? primary.serviceName() : null;
        String rootCause = primary != null
                ? primary.description()
                : "No significant issues detected";

        return new FlowExplanation(
                summary,
                bottleneckService,
                rootCause,
                List.of("Monitor service health", "Review error logs", "Check resource utilization"),
                primary != null ? primary.severity().name().toLowerCase() : "low");
    }

    /**
     * AI-generated flow explanation response
     */
    public record FlowExplanation(
            String summary,
            String bottleneckService,
            String rootCause,
            List<String> recommendations,
            String estimatedImpact) {
    }
}
