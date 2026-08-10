package com.stage.backend.dto.login;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}
