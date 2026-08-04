package com.stage.backend.kafka.producer;

import com.stage.backend.kafka.config.KafkaProperties;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.kafka.validation.CodingChallengeEventValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.support.SendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j

@ConditionalOnProperty(name = "codepulse.kafka.enabled", havingValue = "true")
public class CodingChallengeKafkaProducer {

    private final KafkaTemplate<String, CodingChallengeEvent> kafkaTemplate;
    private final KafkaProperties kafkaProperties;
    private final CodingChallengeEventValidator validator;

    public CompletableFuture<SendResult<String, CodingChallengeEvent>> publish(CodingChallengeEvent event) {
        validator.validate(event);

        String topic = kafkaProperties.topics().codingChallenges();
        String key = String.valueOf(event.test().id());

        log.info("Publishing coding challenge id={} to topic={}", event.test().id(), topic);

        return kafkaTemplate.send(topic, key, event)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error(
                                "Failed to publish coding challenge id={} to topic={}",
                                event.test().id(),
                                topic,
                                throwable
                        );
                        return;
                    }

                    log.debug(
                            "Published coding challenge id={} to partition={} offset={}",
                            event.test().id(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
