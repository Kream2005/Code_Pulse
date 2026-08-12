package com.stage.backend.config.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "codepulse.notification")
public record NotificationProperties(
        boolean enabled,
        String from,
        String frontendBaseUrl,
        /** When set, challenge notification emails are delivered to this address. */
        String to,
        @DefaultValue Relance relance,
        @DefaultValue("false") boolean embeddedSmtp
) {
    public record Relance(
            @DefaultValue("false") boolean enabled,
            @DefaultValue("24h") Duration delay,
            @DefaultValue("15m") Duration checkInterval,
            @DefaultValue("45s") Duration initialDelay,
            @DefaultValue("3") int max
    ) {}
}
