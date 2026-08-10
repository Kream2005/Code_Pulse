package com.stage.backend.dto.demande;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetTemporaryPasswordRequest(
        @NotBlank @Size(min = 8, max = 100) String temporaryPassword
) {}
