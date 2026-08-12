package com.stage.backend.dto.notification;

import java.time.Instant;

public record CapturedMailDto(
        String from,
        String to,
        String subject,
        String body,
        Instant sentAt
) {}
