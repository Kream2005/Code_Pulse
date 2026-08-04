package com.stage.backend.dto.analytics;

public record ChallengeStatisticsResponse(
        Long challengeId,
        String titre,
        Double averageScore,
        Long feedbackCount
) {}
