package com.stage.backend.dto.analytics;

public record ChallengeAdminDashboardKpiResponse(
        long challengesActifs,
        long challengesArchives,
        long notificationsTotal,
        long notificationsEnAttente,
        long feedbacksRecus,
        double noteMoyenne
) {}
