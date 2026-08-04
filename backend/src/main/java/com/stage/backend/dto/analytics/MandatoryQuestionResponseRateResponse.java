package com.stage.backend.dto.analytics;

public record MandatoryQuestionResponseRateResponse(
        Long questionId,
        String libelle,
        Double responseRate
) {}
