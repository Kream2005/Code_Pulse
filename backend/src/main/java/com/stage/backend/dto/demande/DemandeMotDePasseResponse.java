package com.stage.backend.dto.demande;

import com.stage.backend.enums.ResultatDemandeMotDePasse;

public record DemandeMotDePasseResponse(
        boolean acceptee,
        ResultatDemandeMotDePasse resultat,
        String emailMasque,
        String message
) {}
