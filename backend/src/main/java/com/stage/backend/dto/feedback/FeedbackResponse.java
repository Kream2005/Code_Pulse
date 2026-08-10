package com.stage.backend.dto.feedback;

import com.stage.backend.enums.StatutFeedback;

import java.time.ZonedDateTime;

public record FeedbackResponse(
        Long id,
        Float noteGlobale,
        String commentaire,
        StatutFeedback statut,
        ZonedDateTime createdAt,
        Long utilisateurId,
        String utilisateurNom,
        String utilisateurPrenom,
        String utilisateurUserName,
        String utilisateurEmail,
        Long codingChallengeId,
        String challengeTitre,
        String challengeTag,
        String challengeDescription,
        boolean challengeSupprime
) {}