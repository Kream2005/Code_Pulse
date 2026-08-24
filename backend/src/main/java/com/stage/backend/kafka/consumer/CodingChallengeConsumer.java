package com.stage.backend.kafka.consumer;

import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.service.codingchallenge.CodingChallengeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "codepulse.kafka.enabled", havingValue = "true")
public class CodingChallengeConsumer {

    private final CodingChallengeService service;

    @KafkaListener(
            topics = "${codepulse.kafka.topics.coding-challenges}",
            groupId = "${codepulse.kafka.consumer-group}",
            containerFactory = "codingChallengeKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload CodingChallengeEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.info(
                "Received coding challenge id={} titre='{}' user_id='{}' userName='{}' from topic={} partition={} offset={}",
                event.test().id(),
                event.test().titre(),
                event.user().id(),
                event.user().userName(),
                topic,
                partition,
                offset
        );

        var result = service.processIncomingChallengeDetailed(event, (int) offset);
        log.info(
                "Kafka ingest result status={} case={} testExternalId={} userExternalId={} errors={}",
                result.status(),
                result.entityCase(),
                result.testExternalId(),
                result.userExternalId(),
                result.errors()
        );
        acknowledgment.acknowledge();
    }
}