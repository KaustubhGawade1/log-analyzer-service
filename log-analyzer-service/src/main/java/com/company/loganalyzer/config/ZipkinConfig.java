package com.company.loganalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for Zipkin integration.
 */
@Configuration
@ConfigurationProperties(prefix = "zipkin")
public class ZipkinConfig {

    private String baseUrl = "http://localhost:9411";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;
    private int defaultLookbackMs = 3600000; // 1 hour
    private int defaultLimit = 100;

    @Bean
    public RestClient zipkinRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getDefaultLookbackMs() {
        return defaultLookbackMs;
    }

    public void setDefaultLookbackMs(int defaultLookbackMs) {
        this.defaultLookbackMs = defaultLookbackMs;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }
}
