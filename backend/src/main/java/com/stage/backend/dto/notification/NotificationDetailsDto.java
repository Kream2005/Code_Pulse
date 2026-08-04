package com.stage.backend.dto.notification;

import com.stage.backend.enums.StatutNotification;
import java.time.ZonedDateTime;

public record NotificationDetailsDto(
        Long id,
        ZonedDateTime dateEnvoi,
        StatutNotification statut,
        String utilisateurNom,
        String utilisateurEmail,
        String challengeTitre
) {}