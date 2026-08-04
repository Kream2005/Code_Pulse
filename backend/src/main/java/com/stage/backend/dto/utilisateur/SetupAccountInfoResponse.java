package com.stage.backend.dto.utilisateur;

// to pre-fill fields in frontend
public record SetupAccountInfoResponse(
        String email,
        String nom,
        String prenom,
        String userName
) {}