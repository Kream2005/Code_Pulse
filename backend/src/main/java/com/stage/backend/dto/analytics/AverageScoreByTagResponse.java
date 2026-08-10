package com.stage.backend.dto.analytics;

public record AverageScoreByTagResponse(
        String tag,
        Double averageScore
) {}
