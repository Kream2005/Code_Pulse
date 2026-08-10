package com.stage.backend.service.codingchallenge;

import com.stage.backend.dto.codingchallenge.CodingChallengeDto;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.Role;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.repository.NotificationRepository;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.kafka.sync.CodingChallengeSyncService;
import com.stage.backend.kafka.validation.CodingChallengeEventValidator;
import com.stage.backend.mapper.CodingChallengeMapper;
import com.stage.backend.repository.CodingChallengeRepository;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import com.stage.backend.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CodingChallengeServiceImp implements CodingChallengeService {

    private final CodingChallengeRepository codingChallengeRepository;
    private final UtilisateurRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CodingChallengeMapper mapper;
    private final CodingChallengeEventValidator eventValidator;
    private final IntegrationLogService integrationLogService;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private CodingChallengeSyncService syncService;

    @Override
    public void processIncomingChallenge(CodingChallengeEvent event) {
        eventValidator.validate(event);

        try {

            CodingChallenge codingChallenge = resolveCodingChallenge(event);
            Utilisateur user = resolveUtilisateur(event);

            userRepository.save(user);

            integrationLogService.logSyncEvent(
                    null,
                    StatutLog.INFO,
                    "User synchronized from Kafka: "
                            + event.user().userName()
                );

            notificationService.notifyChallengeCompletion(user, codingChallenge);

            log.info(
                    "Challenge completion processed: test='{}' user='{}'",
                    event.test().titre(),
                    event.user().email()
            );

        } catch (RuntimeException exception) {

            integrationLogService.logSyncEvent(
                    null,
                    StatutLog.ERREUR,
                    "Failed to synchronize coding challenge from Kafka: "
                            + exception.getMessage()
            );

            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CodingChallengeDto getCodingChallenge(Long challengeId) {
        CodingChallenge challenge = codingChallengeRepository.findById(challengeId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Coding challenge with id: " + challengeId + " was not found"
                        )
                );

        return mapper.toCodingChallengeDto(challenge);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodingChallengeDto> getAllCodingChallenges() {
        return codingChallengeRepository.findBySupprimeFalse()
                .stream()
                .map(mapper::toCodingChallengeDto)
                .toList();
    }

    @Override
    public boolean supprimerCodingChallenge(Long challengeId) {
        return codingChallengeRepository.findById(challengeId).map(challenge -> {
            challenge.setSupprime(true);
            codingChallengeRepository.save(challenge);
            notificationRepository.findByCodingChallengeId(challengeId).forEach(n -> {
                n.setSupprime(true);
                notificationRepository.save(n);
            });
            integrationLogService.logEvent(
                    TypeLog.GESTION_CHALLENGE,
                    StatutLog.SUCCES,
                    "Coding challenge soft-deleted (feedbacks kept): id=" + challengeId
                            + " titre=" + challenge.getTitre(),
                    challengeId
            );
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CodingChallengeDto> getCodingChallengesPage(int page, int size) {
        return codingChallengeRepository.findBySupprimeFalse(PageRequest.of(page, size))
                .map(mapper::toCodingChallengeDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CodingChallengeDto> searchCodingChallenges(String keyword, String tag, int page, int size) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedTag = tag == null ? "" : tag.trim();
        return codingChallengeRepository
                .search(normalizedKeyword, normalizedTag, PageRequest.of(page, size))
                .map(mapper::toCodingChallengeDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctTags() {
        return codingChallengeRepository.findDistinctTags();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodingChallengeDto> rechercherChallengesByTitre(String titre) {
        return codingChallengeRepository.findByTitreContainingIgnoreCase(titre)
                .stream()
                .map(mapper::toCodingChallengeDto)
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<CodingChallengeDto> getChallengesByDescription(String description) {
        return codingChallengeRepository.findByDescriptionContainingIgnoreCase(description)
                .stream()
                .map(mapper::toCodingChallengeDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodingChallengeDto> getChallengesByDuree(Integer duree) {
        return codingChallengeRepository.findByDuree(duree)
                .stream()
                .map(mapper::toCodingChallengeDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodingChallengeDto> getChallengesByDateCompletion(ZonedDateTime dateCompletion) {
        return codingChallengeRepository.findByDateCompletion(dateCompletion)
                .stream()
                .map(mapper::toCodingChallengeDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countCodingChallenges() {
        return codingChallengeRepository.countBySupprimeFalse();
    }

    @Override
    public int synchroniserChallenges() {
        if (syncService == null) {
            log.warn("Challenge sync service is not available.");
            return 0;
        }
        log.info("Triggering coding challenges synchronization");
        return syncService.synchroniserChallenges();
    }

    @Override
    public int ingestBatch(List<CodingChallengeEvent> events) {
        if (events == null || events.isEmpty()) {
            return 0;
        }
        for (CodingChallengeEvent event : events) {
            processIncomingChallenge(event);
        }
        return events.size();
    }

    private CodingChallenge resolveCodingChallenge(CodingChallengeEvent event) {
        Optional<CodingChallenge> existing =
                codingChallengeRepository.findByExternalId(event.test().id());

        if (existing.isPresent()) {
            return existing.get();
        }

        CodingChallenge codingChallenge = new CodingChallenge();
        codingChallenge.setExternalId(event.test().id());
        codingChallenge.setTitre(event.test().titre());
        codingChallenge.setDescription(event.test().description());
        codingChallenge.setTag(event.test().tag());
        codingChallenge.setDuree(event.test().duree());
        codingChallenge.setCodeUrl(event.test().codeUrl());
        codingChallenge.setParameter(event.test().parameter());
        codingChallenge.setDateCompletion(ZonedDateTime.now());

        CodingChallenge saved = codingChallengeRepository.save(codingChallenge);

        integrationLogService.logSyncEvent(
                saved.getId(),
                StatutLog.INFO,
                "Coding challenge synchronized from Kafka: " + event.test().titre()
        );

        return saved;
    }

    private Utilisateur resolveUtilisateur(CodingChallengeEvent event) {
        var incoming = event.user();

        Optional<Utilisateur> existing = userRepository.findByExternalId(incoming.id())
                .or(() -> userRepository.findByEmail(incoming.email()))
                .or(() -> {
                    if (incoming.userName() == null || incoming.userName().isBlank()) {
                        return Optional.empty();
                    }
                    return userRepository.findByUserName(incoming.userName());
                });

        if (existing.isPresent()) {
            Utilisateur user = existing.get();
            if (user.getExternalId() == null) {
                user.setExternalId(incoming.id());
            }
            if (incoming.nom() != null) {
                user.setNom(incoming.nom());
            }
            if (incoming.prenom() != null) {
                user.setPrenom(incoming.prenom());
            }
            if (incoming.email() != null) {
                user.setEmail(incoming.email());
            }
            if (incoming.status() != null) {
                user.setStatus(incoming.status());
            }
            assignUserName(user, incoming.userName(), incoming.id());
            return user;
        }

        Utilisateur user = new Utilisateur();
        user.setExternalId(incoming.id());
        user.setNom(incoming.nom());
        user.setPrenom(incoming.prenom());
        user.setEmail(incoming.email());
        user.setStatus(incoming.status());
        user.setRole(Role.USER);
        user.setCompteComplet(false);
        user.setPassword(null);
        user.setSetupToken(UUID.randomUUID().toString());
        user.setSetupTokenExpiresAt(ZonedDateTime.now().plusHours(24));
        assignUserName(user, incoming.userName(), incoming.id());
        return user;
    }

    private void assignUserName(Utilisateur user, String desired, Long externalId) {
        if (desired == null || desired.isBlank()) {
            if (user.getUserName() == null || user.getUserName().isBlank()) {
                user.setUserName("user." + externalId);
            }
            return;
        }

        if (desired.equals(user.getUserName())) {
            return;
        }

        Optional<Utilisateur> holder = userRepository.findByUserName(desired);
        if (holder.isEmpty() || (user.getId() != null && holder.get().getId().equals(user.getId()))) {
            user.setUserName(desired);
            return;
        }

        String unique = desired + "." + externalId;
        Optional<Utilisateur> uniqueHolder = userRepository.findByUserName(unique);
        if (uniqueHolder.isEmpty()
                || (user.getId() != null && uniqueHolder.get().getId().equals(user.getId()))) {
            user.setUserName(unique);
        }
    }
}