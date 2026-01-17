package com.company.loganalyzer.analysis;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class LogNormalizer {

    private static final Pattern UUID_PATTERN = Pattern
            .compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    public String normalize(String message) {
        if (message == null) {
            return "";
        }
        String normalized = UUID_PATTERN.matcher(message).replaceAll("<UUID>");
        normalized = IP_PATTERN.matcher(normalized).replaceAll("<IP>");
        normalized = NUMBER_PATTERN.matcher(normalized).replaceAll("<NUM>");
        return normalized;
    }
}
