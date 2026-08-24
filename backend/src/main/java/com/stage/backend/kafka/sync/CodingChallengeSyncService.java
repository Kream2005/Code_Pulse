package com.stage.backend.kafka.sync;

import com.stage.backend.dto.codingchallenge.ChallengeIngestItemResult;
import com.stage.backend.dto.codingchallenge.ChallengeSyncResponse;
import com.stage.backend.enums.IngestItemStatus;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.kafka.config.ExternalApiProperties;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.kafka.producer.CodingChallengeKafkaProducer;
import com.stage.backend.service.codingchallenge.CodingChallengeService;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
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
    private final boolean kafkaEnabled;

    public CodingChallengeSyncService(
            ExternalApiProperties externalApiProperties,
            RestClient externalApiRestClient,
            ObjectProvider<CodingChallengeKafkaProducer> kafkaProducer,
            @Lazy CodingChallengeService codingChallengeService,
            IntegrationLogService integrationLogService,
            @Value("${codepulse.kafka.enabled:false}") boolean kafkaEnabled
    ) {
        this.externalApiProperties = externalApiProperties;
        this.externalApiRestClient = externalApiRestClient;
        this.kafkaProducer = kafkaProducer;
        this.codingChallengeService = codingChallengeService;
        this.integrationLogService = integrationLogService;
        this.kafkaEnabled = kafkaEnabled;
    }

    public ChallengeSyncResponse synchroniserChallenges() {
        if (!externalApiProperties.enabled()) {
            String message = "L'API externe des coding challenges est désactivée. "
                    + "Définissez codepulse.external-api.enabled=true pour lancer une synchronisation.";
            log.warn(message);
            integrationLogService.logSyncEvent(null, StatutLog.WARNING, message);
            return new ChallengeSyncResponse(
                    "desactive",
                    false,
                    kafkaEnabled,
                    0,
                    0,
                    0,
                    0,
                    message,
                    List.of()
            );
        }

        log.info("Récupération des coding challenges depuis {}", externalApiProperties.codingChallengesUrl());

        try {
            List<CodingChallengeEvent> challenges = fetchChallengesFromApi();

            if (challenges.isEmpty()) {
                integrationLogService.logSyncEvent(
                        null,
                        StatutLog.INFO,
                        "L'API externe n'a retourné aucun coding challenge"
                );
                return new ChallengeSyncResponse(
                        kafkaEnabled ? "kafka" : "direct",
                        true,
                        kafkaEnabled,
                        0,
                        0,
                        0,
                        0,
                        "Le publisher a retourné un lot vide — rien à synchroniser",
                        List.of()
                );
            }

            CodingChallengeKafkaProducer producer = kafkaProducer.getIfAvailable();
            if (producer != null) {
                for (CodingChallengeEvent challenge : challenges) {
                    producer.publish(challenge);
                }
                log.info("{} coding challenge(s) publié(s) sur Kafka pour ingestion", challenges.size());
                return new ChallengeSyncResponse(
                        "kafka",
                        true,
                        true,
                        challenges.size(),
                        challenges.size(),
                        challenges.size(),
                        0,
                        challenges.size() + " événement(s) publié(s) sur Kafka — le consommateur les traitera",
                        List.of()
                );
            }

            List<ChallengeIngestItemResult> results = new ArrayList<>();
            int succeeded = 0;
            int failed = 0;
            for (int i = 0; i < challenges.size(); i++) {
                ChallengeIngestItemResult item = codingChallengeService.processIncomingChallengeDetailed(
                        challenges.get(i),
                        i
                );
                results.add(item);
                if (item.status() == IngestItemStatus.SUCCESS) {
                    succeeded++;
                } else {
                    failed++;
                }
            }
            log.info("{} coding challenge(s) ingéré(s) directement (Kafka désactivé)", challenges.size());
            return new ChallengeSyncResponse(
                    "direct",
                    true,
                    false,
                    challenges.size(),
                    succeeded,
                    succeeded,
                    failed,
                    succeeded + "/" + challenges.size()
                            + " événement(s) traité(s) directement (sans producteur Kafka)",
                    results
            );
        } catch (RestClientException exception) {
            String message = "Échec de récupération des coding challenges depuis l'API externe : "
                    + exception.getMessage();
            log.error(message, exception);
            integrationLogService.logSyncEvent(null, StatutLog.ERREUR, message);
            return new ChallengeSyncResponse(
                    "erreur",
                    true,
                    kafkaEnabled,
                    0,
                    0,
                    0,
                    0,
                    message,
                    List.of()
            );
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
