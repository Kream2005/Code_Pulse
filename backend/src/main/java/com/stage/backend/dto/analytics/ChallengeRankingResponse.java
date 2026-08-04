package com.stage.backend.dto.analytics;

public record ChallengeRankingResponse(
        Long challengeId,
        String titre,
        Double metricValue,
        String rankingType
) {}
