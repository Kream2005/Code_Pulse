package com.stage.backend.dto.common;

public record SuppressionResponse(
        boolean supprime,
        Long entiteId,
        String typeEntite,
        boolean suppressionDouce,
        int enregistrementsLiesAffectes,
        String message
) {}
