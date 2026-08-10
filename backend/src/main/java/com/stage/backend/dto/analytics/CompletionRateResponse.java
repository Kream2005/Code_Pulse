package com.stage.backend.dto.analytics;

public record CompletionRateResponse(
        String tag,
        Double completionRate
) {}
