package com.stage.backend.dto.utilisateur;

import com.stage.backend.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUtilisateurRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @Email @NotBlank String email,
        @NotBlank @Size(min = 8) String rawPassword,
        @NotNull Role role
) {}
