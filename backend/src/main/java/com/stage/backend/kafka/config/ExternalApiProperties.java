package com.stage.backend.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "codepulse.external-api")
public record ExternalApiProperties (
        boolean enabled,
        String codingChallengesUrl,
        Duration connectTimeout,
        Duration readTimeout
) {}
