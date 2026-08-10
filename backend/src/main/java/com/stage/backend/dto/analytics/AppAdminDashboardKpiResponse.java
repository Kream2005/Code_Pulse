package com.stage.backend.dto.analytics;

public record AppAdminDashboardKpiResponse(
        long utilisateursStaff,
        long utilisateursCandidats,
        long questionsActives,
        long demandesReinitEnAttente,
        long logsErreur,
        long challengesActifs,
        long feedbacksSoumis,
        boolean kafkaEnabled,
        boolean notificationEnabled
) {}
