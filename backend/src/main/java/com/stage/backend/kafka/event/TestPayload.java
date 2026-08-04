package com.stage.backend.kafka.event;

public record TestPayload(
        Long id,
        String titre,
        String description,
        String tag,
        Integer duree,
        String codeUrl,
        Boolean parameter
) {}