package com.company.loganalyzer.analysis;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class ErrorClusterer {

    public String generateClusterId(String normalizedMessage, String stackTrace) {
        String contentToHash = normalizedMessage;

        if (stackTrace != null && !stackTrace.isEmpty()) {
            // Take the first 3 lines of the stack trace effectively
            String[] lines = stackTrace.split("\n");
            StringBuilder sb = new StringBuilder(normalizedMessage);
            for (int i = 0; i < Math.min(lines.length, 3); i++) {
                sb.append(lines[i]);
            }
            contentToHash = sb.toString();
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(contentToHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
