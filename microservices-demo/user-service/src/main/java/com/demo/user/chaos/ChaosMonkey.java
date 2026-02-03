package com.demo.user.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class ChaosMonkey {

    private static final Logger log = LoggerFactory.getLogger(ChaosMonkey.class);
    private final Random random = new Random();

    @Value("${chaos.latency.enabled:false}")
    private boolean latencyEnabled;

    @Value("${chaos.latency.min-ms:100}")
    private int latencyMinMs;

    @Value("${chaos.latency.max-ms:2000}")
    private int latencyMaxMs;

    @Value("${chaos.error.enabled:false}")
    private boolean errorEnabled;

    @Value("${chaos.error.rate:0.1}")
    private double errorRate;

    public void maybeInjectChaos() {
        maybeInjectLatency();
        maybeInjectError();
    }

    private void maybeInjectLatency() {
        if (latencyEnabled) {
            int delay = latencyMinMs + random.nextInt(latencyMaxMs - latencyMinMs);
            log.warn("Chaos: Injecting {}ms latency", delay);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void maybeInjectError() {
        if (errorEnabled && random.nextDouble() < errorRate) {
            log.error("Chaos: Injecting simulated failure");
            throw new RuntimeException("Simulated chaos failure");
        }
    }
}
