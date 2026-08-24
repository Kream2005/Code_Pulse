package com.stage.backend.dto.notification;

import com.stage.backend.enums.ResultatLivraisonEmail;

public record NotificationCreationResult(
        NotificationDto notification,
        boolean dejaExistante,
        ResultatLivraisonEmail livraisonEmail,
        String urlAction,
        String message
) {
    public NotificationEnvoiResponse toEnvoiResponse() {
        return new NotificationEnvoiResponse(
                dejaExistante,
                !dejaExistante,
                notification,
                livraisonEmail,
                urlAction,
                message
        );
    }
}
