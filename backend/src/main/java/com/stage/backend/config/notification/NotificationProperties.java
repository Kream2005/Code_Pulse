package com.stage.backend.config.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codepulse.notification")
public record NotificationProperties(
        boolean enabled,
        String from,
        String frontendBaseUrl,
        /** When set, challenge notification emails are delivered to this address. */
        String to
) {}
