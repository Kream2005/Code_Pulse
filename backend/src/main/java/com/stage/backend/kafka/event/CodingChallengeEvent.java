package com.stage.backend.kafka.event;

public record CodingChallengeEvent (
        UserPayload user,
        TestPayload test
) {}
