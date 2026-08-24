package com.stage.backend.dto.codingchallenge;

public record ChallengeDeleteResponse(
        boolean deleted,
        Long challengeId,
        int notificationsSoftDeleted,
        String message
) {}
