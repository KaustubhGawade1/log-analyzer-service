package com.company.loganalyzer.model;

import java.util.List;

public record AnalysisResult(
        String normalizedMessage,
        String clusterId,
        List<AnomalyType> anomalies,
        boolean isNewPattern) {
}
