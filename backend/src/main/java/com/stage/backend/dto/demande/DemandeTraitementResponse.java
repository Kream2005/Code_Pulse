package com.stage.backend.dto.demande;

import com.stage.backend.enums.StatutDemandeReinit;

public record DemandeTraitementResponse(
        DemandeReinitialisationDto demande,
        boolean emailEnvoye,
        String urlAction,
        StatutDemandeReinit statutPrecedent,
        String message
) {}
