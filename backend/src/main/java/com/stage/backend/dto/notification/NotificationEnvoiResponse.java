package com.stage.backend.dto.notification;

import com.stage.backend.enums.ResultatLivraisonEmail;

public record NotificationEnvoiResponse(
        boolean dejaExistante,
        boolean creee,
        NotificationDto notification,
        ResultatLivraisonEmail livraisonEmail,
        String urlAction,
        String message
) {}
