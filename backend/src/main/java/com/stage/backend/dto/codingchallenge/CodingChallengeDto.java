package com.stage.backend.dto.codingchallenge;

import java.time.ZonedDateTime;

public record CodingChallengeDto(
        Long id,
        String titre,
        String description,
        String tag,
        Integer duree,
        String codeUrl,
        Boolean parameter,
        ZonedDateTime dateCompletion,
        boolean supprime
) {}
