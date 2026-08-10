package com.stage.backend.dto.integrationlog;

import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;

import java.time.ZonedDateTime;

public record CreateIntegrationLogRequest(
        TypeLog type,
        StatutLog statut,
        String message,
        ZonedDateTime date
) {}
