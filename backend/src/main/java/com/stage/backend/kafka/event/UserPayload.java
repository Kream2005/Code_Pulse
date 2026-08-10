package com.stage.backend.kafka.event;

public record UserPayload(
        Long id,
        String nom,
        String prenom,
        String userName,
        String email,
        Boolean status
) {}
