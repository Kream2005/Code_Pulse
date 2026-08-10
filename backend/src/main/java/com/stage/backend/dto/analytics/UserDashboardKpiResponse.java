package com.stage.backend.dto.analytics;

public record UserDashboardKpiResponse(
        long notificationsTotal,
        long notificationsPending,
        long feedbacksSubmitted
) {}
