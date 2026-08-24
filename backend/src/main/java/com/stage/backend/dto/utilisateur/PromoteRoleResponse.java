package com.stage.backend.dto.utilisateur;

import com.stage.backend.enums.Role;

public record PromoteRoleResponse(
        UtilisateurDto utilisateur,
        Role rolePrecedent,
        Role nouveauRole,
        String message
) {}
