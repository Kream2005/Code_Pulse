package com.stage.backend.dto.notification;

import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
        @NotNull Long utilisateurId,
        @NotNull Long codingChallengeId
) {}
