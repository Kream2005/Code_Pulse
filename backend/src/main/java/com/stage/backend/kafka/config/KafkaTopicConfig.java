package com.stage.backend.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "codepulse.kafka.enabled", havingValue = "true")
public class KafkaTopicConfig {

    @Bean
    NewTopic codingChallengeTopic(KafkaProperties kafkaProperties) {

        return TopicBuilder.name(kafkaProperties.topics().codingChallenges())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic codingChallengeDltTopic(KafkaProperties kafkaProperties) {

        return TopicBuilder.name(kafkaProperties.topics().codingChallengesDlt())
                .partitions(1)
                .replicas(1)
                .build();
    }
}
