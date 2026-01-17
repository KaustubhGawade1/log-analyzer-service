package com.company.loganalyzer.analysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogNormalizerTest {

    private final LogNormalizer normalizer = new LogNormalizer();

    @Test
    void shouldNormalizeNumbers() {
        String input = "Response time was 123ms";
        assertEquals("Response time was <NUM>ms", normalizer.normalize(input));
    }

    @Test
    void shouldNormalizeIPAddress() {
        String input = "Connection from 192.168.1.1 failed";
        assertEquals("Connection from <IP> failed", normalizer.normalize(input));
    }

    @Test
    void shouldNormalizeUUID() {
        String input = "Request ID: 550e8400-e29b-41d4-a716-446655440000 processed";
        assertEquals("Request ID: <UUID> processed", normalizer.normalize(input));
    }

    @Test
    void shouldNormalizeComplexMix() {
        String input = "User 550e8400-e29b-41d4-a716-446655440000 at 10.0.0.1 failed 3 times";
        assertEquals("User <UUID> at <IP> failed <NUM> times", normalizer.normalize(input));
    }
}
