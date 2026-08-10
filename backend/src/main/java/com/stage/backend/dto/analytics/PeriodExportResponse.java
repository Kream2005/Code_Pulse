package com.stage.backend.dto.analytics;

import java.time.ZonedDateTime;

public record PeriodExportResponse(
        ZonedDateTime startDate,
        ZonedDateTime endDate,
        String exportFormat,
        byte[] data
) {}
