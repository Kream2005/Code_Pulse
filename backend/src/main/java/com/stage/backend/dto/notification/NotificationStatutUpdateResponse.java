package com.stage.backend.dto.notification;

import com.stage.backend.enums.StatutNotification;

public record NotificationStatutUpdateResponse(
        Long notificationId,
        StatutNotification statutPrecedent,
        StatutNotification nouveauStatut,
        boolean misAJour,
        String message
) {}
