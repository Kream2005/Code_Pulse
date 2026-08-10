package com.stage.backend.dto.demande;

import com.stage.backend.enums.StatutDemandeReinit;

import java.time.ZonedDateTime;

public record DemandeReinitialisationDto(
        Long id,
        String email,
        Long utilisateurId,
        String utilisateurNom,
        String utilisateurPrenom,
        StatutDemandeReinit statut,
        ZonedDateTime dateDemande,
        ZonedDateTime dateTraitement,
        Long traiteParId,
        String traiteParEmail
) {}
