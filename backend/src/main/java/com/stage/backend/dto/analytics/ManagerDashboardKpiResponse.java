package com.stage.backend.dto.analytics;

public record ManagerDashboardKpiResponse(
        double tauxParticipation,
        long challengesTotal,
        long feedbacksSoumis,
        double noteMoyenneGlobale,
        long notificationsEnAttente,
        long tagsCouverts
) {}
