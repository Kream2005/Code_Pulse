package com.stage.backend.dto.analytics;

public record TagStatisticsResponse(
        String tag,
        Double averageScore,
        Long feedbackCount,
        Long challengeCount,
        Double completionRate
) {}
