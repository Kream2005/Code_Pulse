package com.stage.backend.dto.utilisateur;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteAccountRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 100) String nom,
        @Size(max = 100) String prenom,
        @Size(max = 100) String userName
) {}
