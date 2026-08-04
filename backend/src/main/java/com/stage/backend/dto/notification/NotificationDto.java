package com.stage.backend.dto.notification;

import com.stage.backend.enums.StatutNotification;
import java.time.ZonedDateTime;

public record NotificationDto(
        Long id,
        ZonedDateTime dateEnvoi,
        StatutNotification statut,
        Long utilisateurId,
        Long codingChallengeId,
        String challengeTitre,
        String challengeTag,
        Integer challengeDuree,
        String challengeDescription
) {}
