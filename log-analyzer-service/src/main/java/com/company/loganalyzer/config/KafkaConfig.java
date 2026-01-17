package com.company.loganalyzer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_APP_LOGS = "app-logs";

    @Bean
    public NewTopic appLogsTopic() {
        return TopicBuilder.name(TOPIC_APP_LOGS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
