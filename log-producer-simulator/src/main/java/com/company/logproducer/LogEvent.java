package com.company.logproducer;

import java.time.Instant;
import java.util.Map;

public record LogEvent(
        String serviceName,
        String level,
        String message,
        String stackTrace,
        Instant timestamp,
        Map<String, String> metadata) {
}
