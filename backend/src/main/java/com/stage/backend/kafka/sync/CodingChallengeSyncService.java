package com.stage.backend.kafka.sync;

import com.stage.backend.enums.StatutLog;
import com.stage.backend.kafka.config.ExternalApiProperties;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.kafka.producer.CodingChallengeKafkaProducer;
import com.stage.backend.service.codingchallenge.CodingChallengeService;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
@Slf4j
public class CodingChallengeSyncService {

    private final ExternalApiProperties externalApiProperties;
    private final RestClient externalApiRestClient;
    private final ObjectProvider<CodingChallengeKafkaProducer> kafkaProducer;
    private final CodingChallengeService codingChallengeService;
    private final IntegrationLogService integrationLogService;

    public CodingChallengeSyncService(
            ExternalApiProperties externalApiProperties,
            RestClient externalApiRestClient,
            ObjectProvider<CodingChallengeKafkaProducer> kafkaProducer,
            @Lazy CodingChallengeService codingChallengeService,
            IntegrationLogService integrationLogService
    ) {
        this.externalApiProperties = externalApiProperties;
        this.externalApiRestClient = externalApiRestClient;
        this.kafkaProducer = kafkaProducer;
        this.codingChallengeService = codingChallengeService;
        this.integrationLogService = integrationLogService;
    }

    public int synchroniserChallenges() {
        if (!externalApiProperties.enabled()) {
            String message = "External coding challenges API is disabled. "
                    + "Set codepulse.external-api.enabled=true to trigger synchronization.";
            log.warn(message);
            integrationLogService.logSyncEvent(null, StatutLog.WARNING, message);
            return 0;
        }

        log.info("Fetching coding challenges from {}", externalApiProperties.codingChallengesUrl());

        try {
            List<CodingChallengeEvent> challenges = fetchChallengesFromApi();

            if (challenges.isEmpty()) {
                integrationLogService.logSyncEvent(
                        null,
                        StatutLog.INFO,
                        "External API returned no coding challenges"
                );
                return 0;
            }

            CodingChallengeKafkaProducer producer = kafkaProducer.getIfAvailable();
            if (producer != null) {
                for (CodingChallengeEvent challenge : challenges) {
                    producer.publish(challenge);
                }
                log.info("Published '{}' coding challenges to Kafka for ingestion", challenges.size());
            } else {
                for (CodingChallengeEvent challenge : challenges) {
                    codingChallengeService.processIncomingChallenge(challenge);
                }
                log.info("Ingested '{}' coding challenges directly (Kafka disabled)", challenges.size());
            }

            return challenges.size();
        } catch (RestClientException exception) {
            String message = "Failed to fetch coding challenges from external API: " + exception.getMessage();
            log.error(message, exception);
            integrationLogService.logSyncEvent(null, StatutLog.ERREUR, message);
            throw exception;
        }
    }

    private List<CodingChallengeEvent> fetchChallengesFromApi() {
        List<CodingChallengeEvent> challenges = externalApiRestClient.get()
                .uri(externalApiProperties.codingChallengesUrl())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return challenges == null ? Collections.emptyList() : challenges;
    }
}
