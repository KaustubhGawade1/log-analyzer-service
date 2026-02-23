package com.demo.order.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class KafkaLogForwarder {

    private static final Logger log = LoggerFactory.getLogger(KafkaLogForwarder.class);
    private static final String TOPIC = "app-logs";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.application.name}")
    private String serviceName;

    public KafkaLogForwarder(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void info(String message) {
        send("INFO", message, null);
    }

    public void warn(String message) {
        send("WARN", message, null);
    }

    public void error(String message) {
        send("ERROR", message, null);
    }

    public void error(String message, Throwable throwable) {
        send("ERROR", message, throwable);
    }

    private void send(String level, String message, Throwable throwable) {
        try {
            Map<String, Object> logEvent = new HashMap<>();
            logEvent.put("serviceName", serviceName);
            logEvent.put("level", level);
            logEvent.put("message", message);
            logEvent.put("stackTrace", throwable != null ? getStackTrace(throwable) : null);
            logEvent.put("timestamp", Instant.now().toString());

            Map<String, String> metadata = new HashMap<>();
            String traceId = MDC.get("traceId");
            String spanId = MDC.get("spanId");
            if (traceId != null)
                metadata.put("traceId", traceId);
            if (spanId != null)
                metadata.put("spanId", spanId);
            logEvent.put("metadata", metadata);

            kafkaTemplate.send(TOPIC, serviceName, logEvent);
        } catch (Exception e) {
            log.debug("Failed to forward log to Kafka: {}", e.getMessage());
        }
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();
        String[] lines = trace.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }
}
