package com.company.loganalyzer.analysis;

import com.company.loganalyzer.model.AnomalyType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnomalyDetector {

    // Simple in-memory sliding window for demonstration.
    // In production, this would use Redis or time-series DB.
    private final Map<String, Deque<Instant>> errorWindows = new ConcurrentHashMap<>();
    private static final int ERROR_THRESHOLD = 5;
    private static final int WINDOW_SECONDS = 60;

    public List<AnomalyType> detectAnomalies(String serviceName, String level) {
        List<AnomalyType> detected = new ArrayList<>();

        if ("ERROR".equalsIgnoreCase(level)) {
            if (isErrorBurst(serviceName)) {
                detected.add(AnomalyType.ERROR_BURST);
            }
        }

        return detected;
    }

    private boolean isErrorBurst(String serviceName) {
        Deque<Instant> times = errorWindows.computeIfAbsent(serviceName, k -> new ArrayDeque<>());
        Instant now = Instant.now();

        synchronized (times) {
            times.addLast(now);
            // Remove errors older than window
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minusSeconds(WINDOW_SECONDS))) {
                times.removeFirst();
            }
            return times.size() > ERROR_THRESHOLD;
        }
    }
}
