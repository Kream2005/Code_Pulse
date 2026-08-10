package com.stage.backend.kafka.config;

import com.stage.backend.kafka.event.CodingChallengeEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@EnableConfigurationProperties({
        com.stage.backend.kafka.config.KafkaProperties.class,
        ExternalApiProperties.class,
})
@Configuration
@ConditionalOnProperty(name = "codepulse.kafka.enabled", havingValue = "true")
public class KafkaProducerConfig {

    @Bean
    ProducerFactory<String, CodingChallengeEvent> codingChallengeProducerFactory(
            KafkaProperties springKafkaProperties
    ) {
        Map<String, Object> properties = new HashMap<>(springKafkaProperties.buildProducerProperties());
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(
                properties,
                new StringSerializer(),
                new JacksonJsonSerializer<>()
        );
    }

    @Bean
    KafkaTemplate<String, CodingChallengeEvent> codingChallengeKafkaTemplate(
            ProducerFactory<String, CodingChallengeEvent> codingChallengeProducerFactory
    ) {
        return new KafkaTemplate<>(codingChallengeProducerFactory);
    }
}
