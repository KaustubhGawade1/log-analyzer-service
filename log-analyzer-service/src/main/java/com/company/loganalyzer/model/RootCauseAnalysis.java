package com.company.loganalyzer.model;

import java.util.List;

public record RootCauseAnalysis(
        String summary,
        String probableRootCause,
        List<String> recommendedActions,
        double confidenceScore) {
}
