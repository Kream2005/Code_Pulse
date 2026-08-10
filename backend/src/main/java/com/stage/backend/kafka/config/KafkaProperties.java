package com.stage.backend.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "codepulse.kafka")
public record KafkaProperties (
        boolean enabled,
        Topics topics,
        String consumerGroup,
        Retry retry
) {

    public record Topics(
            String codingChallenges,
            String codingChallengesDlt
    ) {}

    public record Retry(
            int maxAttempts,
            long backoffMs
    ) {}
}
