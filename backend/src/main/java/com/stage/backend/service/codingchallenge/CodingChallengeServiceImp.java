package com.stage.backend.service.codingchallenge;

import com.stage.backend.dto.notification.NotificationCreationResult;
import com.stage.backend.dto.codingchallenge.ChallengeDeleteResponse;
import com.stage.backend.dto.codingchallenge.ChallengeIngestBatchResponse;
import com.stage.backend.dto.codingchallenge.ChallengeIngestItemResult;
import com.stage.backend.dto.codingchallenge.ChallengeSyncResponse;
import com.stage.backend.dto.codingchallenge.CodingChallengeDto;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.IngestEntityCase;
import com.stage.backend.enums.IngestItemStatus;
import com.stage.backend.enums.Role;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.exception.ChallengeIngestConflictException;
import com.stage.backend.kafka.event.CodingChallengeEvent;
import com.stage.backend.kafka.event.UserPayload;
import com.stage.backend.kafka.exception.InvalidCodingChallengeEventException;
import com.stage.backend.kafka.sync.CodingChallengeSyncService;
import com.stage.backend.kafka.validation.CodingChallengeEventValidator;
import com.stage.backend.mapper.CodingChallengeMapper;
import com.stage.backend.repository.CodingChallengeRepository;
import com.stage.backend.repository.NotificationRepository;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import com.stage.backend.service.notification.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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

    @Autowired
    @Lazy
    private CodingChallengeService self;

    @Override
    public void processIncomingChallenge(CodingChallengeEvent event) {
        self.processIncomingChallengeDetailed(event, 0);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChallengeIngestItemResult processIncomingChallengeDetailed(CodingChallengeEvent event, int index) {
        ChallengeIngestItemResult.CodingChallengeEventRef ref = toRef(event);
        try {
            eventValidator.validate(event);

            Optional<CodingChallenge> challengeBefore = codingChallengeRepository.findByExternalId(event.test().id());
            boolean challengeAlreadyExisted = challengeBefore.isPresent();

            UserLookup beforeUser = lookupUser(event.user());
            boolean userAlreadyExisted = beforeUser.matchedByExternalId()
                    || beforeUser.matchedByEmail()
                    || beforeUser.matchedByUserName();

            assertNoUserIdentityConflict(event.user(), beforeUser);

            CodingChallenge codingChallenge = resolveCodingChallenge(event, challengeBefore);
            UserResolveOutcome userOutcome = resolveUtilisateur(event, beforeUser);

            userRepository.save(userOutcome.user());

            IngestEntityCase entityCase = toEntityCase(challengeAlreadyExisted, userAlreadyExisted);

            var notificationOutcome = notificationService.notifyChallengeCompletion(
                    userOutcome.user(),
                    codingChallenge
            );
            boolean notificationAlreadyExisted = notificationOutcome.dejaExistante();
            boolean notificationCreated = !notificationAlreadyExisted;

            List<String> messages = buildSuccessMessages(
                    entityCase,
                    challengeAlreadyExisted,
                    userAlreadyExisted,
                    notificationCreated,
                    notificationAlreadyExisted,
                    userOutcome.fieldsUpdated(),
                    notificationOutcome
            );

            integrationLogService.logSyncEvent(
                    codingChallenge.getId(),
                    StatutLog.SUCCES,
                    "Ingestion OK — cas=" + entityCase
                            + " testExternalId=" + event.test().id()
                            + " userExternalId=" + event.user().id()
            );

            log.info(
                    "Ingestion challenge OK [{}] cas={} test='{}' user='{}'",
                    index,
                    entityCase,
                    event.test().titre(),
                    event.user().email()
            );

            return new ChallengeIngestItemResult(
                    index,
                    IngestItemStatus.SUCCESS,
                    entityCase,
                    event.test().id(),
                    event.test().titre(),
                    event.user().id(),
                    event.user().email(),
                    event.user().userName(),
                    codingChallenge.getId(),
                    userOutcome.user().getId(),
                    challengeAlreadyExisted,
                    userAlreadyExisted,
                    notificationCreated,
                    notificationAlreadyExisted,
                    userOutcome.fieldsUpdated(),
                    messages,
                    List.of(),
                    null
            );
        } catch (InvalidCodingChallengeEventException exception) {
            return failAndLog(index, ref, "ERREUR_VALIDATION", exception.getMessage());
        } catch (ChallengeIngestConflictException exception) {
            return failAndLog(index, ref, exception.getErrorCode(), exception.getMessage());
        } catch (RuntimeException exception) {
            return failAndLog(index, ref, "ERREUR_INGESTION", exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CodingChallengeDto getCodingChallenge(Long challengeId) {
        CodingChallenge challenge = codingChallengeRepository.findById(challengeId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Coding challenge introuvable : id=" + challengeId
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
    public ChallengeDeleteResponse supprimerCodingChallenge(Long challengeId) {
        Optional<CodingChallenge> optional = codingChallengeRepository.findById(challengeId);
        if (optional.isEmpty()) {
            return new ChallengeDeleteResponse(
                    false,
                    challengeId,
                    0,
                    "Coding challenge introuvable : id=" + challengeId
            );
        }
        CodingChallenge challenge = optional.get();
        challenge.setSupprime(true);
        codingChallengeRepository.save(challenge);

        var notifications = notificationRepository.findByCodingChallengeId(challengeId);
        notifications.forEach(n -> {
            n.setSupprime(true);
            notificationRepository.save(n);
        });

        integrationLogService.logEvent(
                TypeLog.GESTION_CHALLENGE,
                StatutLog.SUCCES,
                "Coding challenge archivé (feedbacks conservés) : id=" + challengeId
                        + " titre=" + challenge.getTitre(),
                challengeId
        );

        return new ChallengeDeleteResponse(
                true,
                challengeId,
                notifications.size(),
                "Challenge archivé ; " + notifications.size() + " notification(s) liée(s) archivée(s)"
        );
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
    public ChallengeSyncResponse synchroniserChallenges() {
        if (syncService == null) {
            return new ChallengeSyncResponse(
                    "indisponible",
                    false,
                    false,
                    0,
                    0,
                    0,
                    0,
                    "Service de synchronisation indisponible dans ce profil",
                    List.of()
            );
        }
        log.info("Triggering coding challenges synchronization");
        return syncService.synchroniserChallenges();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChallengeIngestBatchResponse ingestBatch(List<CodingChallengeEvent> events) {
        if (events == null || events.isEmpty()) {
            return new ChallengeIngestBatchResponse(0, 0, 0, Map.of(), List.of());
        }

        List<ChallengeIngestItemResult> items = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;
        Map<String, Integer> caseCounts = new HashMap<>();

        for (int i = 0; i < events.size(); i++) {
            ChallengeIngestItemResult item = self.processIncomingChallengeDetailed(events.get(i), i);
            items.add(item);
            if (item.status() == IngestItemStatus.SUCCESS) {
                succeeded++;
                if (item.entityCase() != null) {
                    caseCounts.merge(item.entityCase().name(), 1, Integer::sum);
                }
            } else {
                failed++;
            }
        }

        return new ChallengeIngestBatchResponse(events.size(), succeeded, failed, caseCounts, items);
    }

    private ChallengeIngestItemResult failAndLog(
            int index,
            ChallengeIngestItemResult.CodingChallengeEventRef ref,
            String errorCode,
            String message
    ) {
        integrationLogService.logSyncEvent(null, StatutLog.ERREUR, message);
        log.warn("Échec ingestion challenge [{}] : {}", errorCode, message);
        return ChallengeIngestItemResult.failed(index, ref, errorCode, message);
    }

    private static ChallengeIngestItemResult.CodingChallengeEventRef toRef(CodingChallengeEvent event) {
        if (event == null) {
            return null;
        }
        Long testId = event.test() != null ? event.test().id() : null;
        String titre = event.test() != null ? event.test().titre() : null;
        Long userId = event.user() != null ? event.user().id() : null;
        String email = event.user() != null ? event.user().email() : null;
        String userName = event.user() != null ? event.user().userName() : null;
        return new ChallengeIngestItemResult.CodingChallengeEventRef(testId, titre, userId, email, userName);
    }

    private static IngestEntityCase toEntityCase(boolean challengeExisted, boolean userExisted) {
        if (challengeExisted && userExisted) {
            return IngestEntityCase.BOTH_EXIST;
        }
        if (challengeExisted) {
            return IngestEntityCase.CHALLENGE_EXISTS_USER_NEW;
        }
        if (userExisted) {
            return IngestEntityCase.USER_EXISTS_CHALLENGE_NEW;
        }
        return IngestEntityCase.BOTH_NEW;
    }

    private static List<String> buildSuccessMessages(
            IngestEntityCase entityCase,
            boolean challengeAlreadyExisted,
            boolean userAlreadyExisted,
            boolean notificationCreated,
            boolean notificationAlreadyExisted,
            List<String> userFieldsUpdated,
            NotificationCreationResult notificationOutcome
    ) {
        List<String> messages = new ArrayList<>();
        messages.add(switch (entityCase) {
            case BOTH_EXIST -> "Challenge et utilisateur existaient déjà — liaison et notification vérifiées";
            case CHALLENGE_EXISTS_USER_NEW -> "Challenge déjà existant — nouvel utilisateur créé et lié";
            case USER_EXISTS_CHALLENGE_NEW -> "Utilisateur déjà existant — nouveau challenge créé et lié";
            case BOTH_NEW -> "Nouveau challenge et nouvel utilisateur créés";
        });
        if (challengeAlreadyExisted) {
            messages.add("Coding challenge trouvé par id externe du test (déjà en base)");
        } else {
            messages.add("Coding challenge inséré depuis le payload event.test");
        }
        if (userAlreadyExisted) {
            messages.add("Utilisateur correspondant à un compte existant (id externe, e-mail ou nom d'utilisateur)");
        } else {
            messages.add("Compte utilisateur créé (jeton de finalisation émis pour compléter le compte)");
        }
        if (!userFieldsUpdated.isEmpty()) {
            messages.add("Champs utilisateur mis à jour : " + String.join(", ", userFieldsUpdated));
        }
        if (notificationAlreadyExisted) {
            messages.add("Notification déjà existante pour cette paire utilisateur/challenge — pas de doublon");
        } else if (notificationCreated) {
            messages.add(notificationOutcome.message());
        }
        return messages;
    }

    private CodingChallenge resolveCodingChallenge(
            CodingChallengeEvent event,
            Optional<CodingChallenge> existing
    ) {
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
                "Coding challenge créé depuis l'ingestion : externalId=" + event.test().id()
                        + " titre=" + event.test().titre()
        );

        return saved;
    }

    private UserResolveOutcome resolveUtilisateur(CodingChallengeEvent event, UserLookup lookup) {
        var incoming = event.user();
        List<String> updatedFields = new ArrayList<>();

        if (lookup.byExternalId().isPresent()) {
            Utilisateur user = lookup.byExternalId().get();
            updatedFields.addAll(applyIncomingProfile(user, incoming));
            return new UserResolveOutcome(user, updatedFields);
        }

        if (lookup.byEmail().isPresent()) {
            Utilisateur user = lookup.byEmail().get();
            if (user.getExternalId() == null) {
                user.setExternalId(incoming.id());
                updatedFields.add("externalId");
            }
            updatedFields.addAll(applyIncomingProfile(user, incoming));
            return new UserResolveOutcome(user, updatedFields);
        }

        if (lookup.byUserName().isPresent()) {
            Utilisateur user = lookup.byUserName().get();
            if (user.getExternalId() == null) {
                user.setExternalId(incoming.id());
                updatedFields.add("externalId");
            }
            updatedFields.addAll(applyIncomingProfile(user, incoming));
            return new UserResolveOutcome(user, updatedFields);
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
        return new UserResolveOutcome(user, List.of("created"));
    }

    private List<String> applyIncomingProfile(Utilisateur user, UserPayload incoming) {
        List<String> updated = new ArrayList<>();
        if (incoming.nom() != null && !incoming.nom().equals(user.getNom())) {
            user.setNom(incoming.nom());
            updated.add("nom");
        }
        if (incoming.prenom() != null && !incoming.prenom().equals(user.getPrenom())) {
            user.setPrenom(incoming.prenom());
            updated.add("prenom");
        }
        if (incoming.email() != null && !incoming.email().equalsIgnoreCase(user.getEmail())) {
            user.setEmail(incoming.email());
            updated.add("email");
        }
        if (incoming.status() != null && !incoming.status().equals(user.getStatus())) {
            user.setStatus(incoming.status());
            updated.add("status");
        }
        if (assignUserName(user, incoming.userName(), incoming.id())) {
            updated.add("userName");
        }
        return updated;
    }

    private UserLookup lookupUser(UserPayload incoming) {
        Optional<Utilisateur> byExternalId = userRepository.findByExternalId(incoming.id());
        Optional<Utilisateur> byEmail = findByEmailIgnoreCase(incoming.email());
        Optional<Utilisateur> byUserName = Optional.empty();
        if (StringUtils.hasText(incoming.userName())) {
            byUserName = userRepository.findByUserName(incoming.userName());
        }
        return new UserLookup(byExternalId, byEmail, byUserName);
    }

    private void assertNoUserIdentityConflict(UserPayload incoming, UserLookup lookup) {
        Optional<Utilisateur> byExternalId = lookup.byExternalId();
        Optional<Utilisateur> byEmail = lookup.byEmail();

        if (byExternalId.isPresent() && byEmail.isPresent()
                && !byExternalId.get().getId().equals(byEmail.get().getId())) {
            Utilisateur external = byExternalId.get();
            Utilisateur emailHolder = byEmail.get();
            throw new ChallengeIngestConflictException(
                    "EMAIL_DEJA_UTILISE_PAR_AUTRE_UTILISATEUR",
                    "L'e-mail '" + incoming.email() + "' est déjà utilisé par l'utilisateur id="
                            + emailHolder.getId() + " (externalId=" + emailHolder.getExternalId()
                            + ") alors que cet événement porte l'id utilisateur externe=" + incoming.id()
                            + ". Un e-mail par utilisateur — deux comptes ne peuvent pas partager le même e-mail.",
                    incoming.id(),
                    incoming.email(),
                    emailHolder.getId(),
                    emailHolder.getExternalId(),
                    emailHolder.getEmail()
            );
        }

        if (byExternalId.isEmpty() && byEmail.isPresent()) {
            Utilisateur emailHolder = byEmail.get();
            if (emailHolder.getExternalId() != null && !emailHolder.getExternalId().equals(incoming.id())) {
                throw new ChallengeIngestConflictException(
                        "EMAIL_DEJA_UTILISE_PAR_AUTRE_UTILISATEUR",
                        "L'e-mail '" + incoming.email() + "' appartient à l'utilisateur externe id="
                                + emailHolder.getExternalId()
                                + " mais l'événement porte l'id utilisateur=" + incoming.id()
                                + ". Impossible d'associer cet e-mail à un autre id externe.",
                        incoming.id(),
                        incoming.email(),
                        emailHolder.getId(),
                        emailHolder.getExternalId(),
                        emailHolder.getEmail()
                );
            }
        }

        if (byExternalId.isPresent() && byEmail.isEmpty() && StringUtils.hasText(incoming.email())) {
            Optional<Utilisateur> emailTaken = findByEmailIgnoreCase(incoming.email());
            if (emailTaken.isPresent() && !emailTaken.get().getId().equals(byExternalId.get().getId())) {
                Utilisateur holder = emailTaken.get();
                throw new ChallengeIngestConflictException(
                        "EMAIL_DEJA_UTILISE_PAR_AUTRE_UTILISATEUR",
                        "Impossible de mettre à jour l'utilisateur externalId=" + incoming.id()
                                + " avec l'e-mail '" + incoming.email()
                                + "' — cet e-mail est déjà utilisé par l'utilisateur id=" + holder.getId()
                                + " (externalId=" + holder.getExternalId() + ")",
                        incoming.id(),
                        incoming.email(),
                        holder.getId(),
                        holder.getExternalId(),
                        holder.getEmail()
                );
            }
        }

        if (StringUtils.hasText(incoming.userName())) {
            Optional<Utilisateur> byUserName = userRepository.findByUserName(incoming.userName());
            if (byExternalId.isPresent() && byUserName.isPresent()
                    && !byExternalId.get().getId().equals(byUserName.get().getId())) {
                Utilisateur holder = byUserName.get();
                throw new ChallengeIngestConflictException(
                        "NOM_UTILISATEUR_DEJA_UTILISE",
                        "Le nom d'utilisateur '" + incoming.userName() + "' est déjà utilisé par l'utilisateur id="
                                + holder.getId() + " (externalId=" + holder.getExternalId() + ")",
                        incoming.id(),
                        incoming.email(),
                        holder.getId(),
                        holder.getExternalId(),
                        holder.getEmail()
                );
            }
        }
    }

    private Optional<Utilisateur> findByEmailIgnoreCase(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    private boolean assignUserName(Utilisateur user, String desired, Long externalId) {
        if (desired == null || desired.isBlank()) {
            if (user.getUserName() == null || user.getUserName().isBlank()) {
                user.setUserName("user." + externalId);
                return true;
            }
            return false;
        }

        if (desired.equals(user.getUserName())) {
            return false;
        }

        Optional<Utilisateur> holder = userRepository.findByUserName(desired);
        if (holder.isEmpty() || (user.getId() != null && holder.get().getId().equals(user.getId()))) {
            user.setUserName(desired);
            return true;
        }

        String unique = desired + "." + externalId;
        Optional<Utilisateur> uniqueHolder = userRepository.findByUserName(unique);
        if (uniqueHolder.isEmpty()
                || (user.getId() != null && uniqueHolder.get().getId().equals(user.getId()))) {
            user.setUserName(unique);
            return true;
        }
        return false;
    }

    private record UserLookup(
            Optional<Utilisateur> byExternalId,
            Optional<Utilisateur> byEmail,
            Optional<Utilisateur> byUserName
    ) {
        boolean matchedByExternalId() {
            return byExternalId().isPresent();
        }

        boolean matchedByEmail() {
            return byEmail().isPresent();
        }

        boolean matchedByUserName() {
            return byUserName().isPresent();
        }
    }

    private record UserResolveOutcome(Utilisateur user, List<String> fieldsUpdated) {}
}
