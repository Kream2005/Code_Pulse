package com.stage.backend.kafka.config;

import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.kafka.exception.InvalidCodingChallengeEventException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@Slf4j
@ConditionalOnProperty(name = "codepulse.kafka.enabled", havingValue = "true")
public class KafkaConsumerConfig {

    @Bean
    ConsumerFactory<String, CodingChallengeEvent> codingChallengeEventConsumerFactory(
            KafkaProperties springKafkaProperties,
            com.stage.backend.kafka.config.KafkaProperties codepulseKafkaProperties
    ) {
        Map<String, Object> properties = new HashMap<>(springKafkaProperties.buildConsumerProperties());

        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        properties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        properties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);
        properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "*");
        properties.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, CodingChallengeEvent.class.getName());
        properties.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, codepulseKafkaProperties.consumerGroup());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, CodingChallengeEvent>
            codingChallengeKafkaListenerContainerFactory(
                    ConsumerFactory<String, CodingChallengeEvent> codingChallengeEventConsumerFactory,
                    DefaultErrorHandler kafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, CodingChallengeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(codingChallengeEventConsumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, CodingChallengeEvent> codingChallengeKafkaTemplate,
            com.stage.backend.kafka.config.KafkaProperties codepulseKafkaProperties
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                codingChallengeKafkaTemplate,
                (consumerRecord, exception) -> {
                    log.error(
                            "Kafka record failed topic={} partition={} offset={}: {}",
                            consumerRecord.topic(),
                            consumerRecord.partition(),
                            consumerRecord.offset(),
                            exception.getMessage(),
                            exception
                    );
                    return new TopicPartition(codepulseKafkaProperties.topics().codingChallengesDlt(), 0);
                }
        );

        FixedBackOff backOff = new FixedBackOff(
                codepulseKafkaProperties.retry().backoffMs(),
                codepulseKafkaProperties.retry().maxAttempts()
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(InvalidCodingChallengeEventException.class);
        return errorHandler;
    }
}
