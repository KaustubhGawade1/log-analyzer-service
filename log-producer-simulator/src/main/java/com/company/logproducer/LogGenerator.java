package com.company.logproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Random;

@Service
public class LogGenerator {

    private static final Logger log = LoggerFactory.getLogger(LogGenerator.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Random random = new Random();
    private static final String TOPIC = "app-logs";

    public LogGenerator(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedRate = 2000)
    public void generateLog() {
        String level = random.nextBoolean() ? "INFO" : (random.nextBoolean() ? "WARN" : "ERROR");
        String message = "Simulated log message " + random.nextInt(1000);
        String service = "payment-service";

        LogEvent event = new LogEvent(
                service,
                level,
                message,
                level.equals("ERROR") ? "java.lang.NullPointerException..." : null,
                Instant.now(),
                Collections.emptyMap());

        log.info("Sending log: {}", event);
        kafkaTemplate.send(TOPIC, service, event);
    }
}
