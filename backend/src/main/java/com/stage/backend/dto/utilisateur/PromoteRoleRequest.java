package com.stage.backend.dto.utilisateur;

import com.stage.backend.enums.Role;
import jakarta.validation.constraints.NotNull;

public record PromoteRoleRequest(
        @NotNull Role role
) {}
