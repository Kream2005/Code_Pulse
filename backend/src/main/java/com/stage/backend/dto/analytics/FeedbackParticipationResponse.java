package com.stage.backend.dto.analytics;

public record FeedbackParticipationResponse(
        Double participationRate,
        Long totalChallenges,
        Long submittedFeedbacks
) {}
