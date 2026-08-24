package com.stage.backend.dto.demande;

import java.time.ZonedDateTime;

public record ResetInfoResponse(
        boolean jetonValide,
        String email,
        ZonedDateTime expireLe,
        String message
) {}
