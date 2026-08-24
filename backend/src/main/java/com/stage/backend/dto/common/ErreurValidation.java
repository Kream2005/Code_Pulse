package com.stage.backend.dto.common;

public record ErreurValidation(
        String champ,
        Long questionId,
        String codeErreur,
        String message
) {}
