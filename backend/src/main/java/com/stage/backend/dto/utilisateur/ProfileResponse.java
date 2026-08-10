package com.stage.backend.dto.utilisateur;

public record ProfileResponse(
        String email,
        String nom,
        String prenom,
        String userName
) {}
