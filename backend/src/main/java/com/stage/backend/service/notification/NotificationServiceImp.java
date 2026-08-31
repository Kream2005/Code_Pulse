package com.stage.backend.service.notification;

import com.stage.backend.config.notification.NotificationProperties;
import com.stage.backend.dto.notification.CreateNotificationRequest;
import com.stage.backend.dto.notification.NotificationCreationResult;
import com.stage.backend.dto.notification.NotificationDto;
import com.stage.backend.dto.notification.NotificationEnvoiResponse;
import com.stage.backend.dto.notification.NotificationStatutUpdateResponse;
import com.stage.backend.email.NotificationEmailSender;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Feedback;
import com.stage.backend.entity.Notification;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.ResultatLivraisonEmail;
import com.stage.backend.enums.StatutFeedback;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.StatutNotification;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.mapper.NotificationMapper;
import com.stage.backend.repository.CodingChallengeRepository;
import com.stage.backend.repository.FeedbackRepository;
import com.stage.backend.repository.NotificationRepository;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import com.stage.backend.util.SetupTokenConstants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImp implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CodingChallengeRepository codingChallengeRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationMapper mapper;
    private final NotificationEmailSender emailSender;
    private final NotificationProperties notificationProperties;
    private final IntegrationLogService integrationLogService;

    @Override
    public NotificationEnvoiResponse envoyerNotification(CreateNotificationRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(request.utilisateurId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Utilisateur introuvable : id=" + request.utilisateurId()
                ));

        CodingChallenge codingChallenge = codingChallengeRepository.findById(request.codingChallengeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Coding challenge introuvable : id=" + request.codingChallengeId()
                ));

        return notifyChallengeCompletion(utilisateur, codingChallenge).toEnvoiResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDto getNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Notification with id: "
                                        + notificationId
                                        + " was not found"
                        )
                );

        return enrichWithFeedback(mapper.toNotificationDto(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByUtilisateur(Long utilisateurId) {
        return notificationRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .map(mapper::toNotificationDto)
                .map(this::enrichWithFeedback)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByUtilisateurPage(Long utilisateurId, int page, int size) {
        return notificationRepository
                .findByUtilisateurIdAndSupprimeFalse(utilisateurId, PageRequest.of(page, size))
                .map(mapper::toNotificationDto)
                .map(this::enrichWithFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> searchNotificationsByUtilisateur(
            Long utilisateurId, String keyword, StatutNotification statut, String tag, int page, int size
    ) {
        String normalized = keyword == null ? "" : keyword.trim();
        String normalizedTag = tag == null ? "" : tag.trim();
        return notificationRepository
                .searchByUtilisateur(utilisateurId, normalized, statut, normalizedTag, PageRequest.of(page, size))
                .map(mapper::toNotificationDto)
                .map(this::enrichWithFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getAllNotifications() {
        return notificationRepository.findAllWithDetails()
                .stream()
                .map(mapper::toNotificationDto)
                .map(this::enrichWithFeedback)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getAllNotificationsPage(int page, int size) {
        return notificationRepository.findAllBy(PageRequest.of(page, size))
                .map(mapper::toNotificationDto)
                .map(this::enrichWithFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByStatut(StatutNotification statut) {
        return notificationRepository.findByStatut(statut)
                .stream()
                .map(mapper::toNotificationDto)
                .map(this::enrichWithFeedback)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByStatutPage(StatutNotification statut, int page, int size) {
        return notificationRepository.findByStatut(statut, PageRequest.of(page, size))
                .map(mapper::toNotificationDto)
                .map(this::enrichWithFeedback);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> searchNotifications(
            String keyword, StatutNotification statut, String tag, int page, int size
    ) {
        String normalized = keyword == null ? "" : keyword.trim();
        String normalizedTag = tag == null ? "" : tag.trim();
        return notificationRepository.search(normalized, statut, normalizedTag, PageRequest.of(page, size))
                .map(mapper::toNotificationDto)
                .map(this::enrichWithFeedback);
    }

    private NotificationDto enrichWithFeedback(NotificationDto dto) {
        if (dto.utilisateurId() == null || dto.codingChallengeId() == null) {
            return withFeedbackFields(dto, null, null);
        }
        Optional<Feedback> feedback = feedbackRepository.findByUtilisateurIdAndCodingChallengeIdAndSupprimeFalse(
                dto.utilisateurId(),
                dto.codingChallengeId()
        );
        return feedback
                .map(f -> withFeedbackFields(dto, f.getId(), f.getStatutFeedback()))
                .orElseGet(() -> withFeedbackFields(dto, null, null));
    }

    private static NotificationDto withFeedbackFields(
            NotificationDto dto,
            Long feedbackId,
            StatutFeedback feedbackStatut
    ) {
        return new NotificationDto(
                dto.id(),
                dto.dateEnvoi(),
                dto.dateDerniereRelance(),
                dto.nombreRelances(),
                dto.statut(),
                dto.utilisateurId(),
                dto.codingChallengeId(),
                dto.challengeTitre(),
                dto.challengeTag(),
                dto.challengeDuree(),
                dto.challengeDescription(),
                feedbackId,
                feedbackStatut
        );
    }

    @Override
    public NotificationStatutUpdateResponse changerStatut(Long notificationId, StatutNotification statut) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Notification introuvable : id=" + notificationId
                        )
                );

        StatutNotification statutPrecedent = notification.getStatut();
        notification.setStatut(statut);
        notificationRepository.save(notification);

        integrationLogService.logEvent(
                TypeLog.ENVOI_NOTIFICATION,
                StatutLog.SUCCES,
                "Notification " + notificationId + " : statut " + statutPrecedent + " → " + statut,
                notification.getCodingChallenge() != null ? notification.getCodingChallenge().getId() : null
        );

        return new NotificationStatutUpdateResponse(
                notificationId,
                statutPrecedent,
                statut,
                true,
                "Statut mis à jour : " + statutPrecedent + " → " + statut
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countNotifications() {
        return notificationRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countNotificationsByStatut(StatutNotification statut) {
        return notificationRepository.countByStatut(statut);
    }

    @Override
    public NotificationCreationResult notifyChallengeCompletion(
            Utilisateur utilisateur,
            CodingChallenge codingChallenge
    ) {
        Optional<Notification> existing = notificationRepository.findByUtilisateurIdAndCodingChallengeId(
                utilisateur.getId(),
                codingChallenge.getId()
        );
        if (existing.isPresent()) {
            integrationLogService.logEvent(
                    TypeLog.ENVOI_NOTIFICATION, StatutLog.INFO,
                    "Notification déjà existante pour " + utilisateur.getEmail()
                            + " et le challenge " + codingChallenge.getTitre(),
                    codingChallenge.getId()
            );
            return new NotificationCreationResult(
                    enrichWithFeedback(mapper.toNotificationDto(existing.get())),
                    true,
                    ResultatLivraisonEmail.NON_APPLICABLE,
                    resolveActionUrl(utilisateur, codingChallenge),
                    "Notification déjà existante pour cet utilisateur et ce challenge — aucun doublon créé"
            );
        }
        Notification notification = new Notification();
        notification.setUtilisateur(utilisateur);
        notification.setCodingChallenge(codingChallenge);
        notification.setDateEnvoi(ZonedDateTime.now());
        notification.setStatut(StatutNotification.EN_ATTENTE);
        Notification saved = notificationRepository.save(notification);
        integrationLogService.logEvent(
                TypeLog.ENVOI_NOTIFICATION, StatutLog.INFO,
                "Notification créée pour " + utilisateur.getEmail()
                        + " et le challenge " + codingChallenge.getTitre(),
                codingChallenge.getId()
        );
        String actionUrl = resolveActionUrl(utilisateur, codingChallenge);
        ResultatLivraisonEmail livraison;
        String message;
        try {
            emailSender.sendChallengeCompletionEmail(utilisateur, codingChallenge, actionUrl);
            if (notificationProperties.enabled()) {
                saved.setStatut(StatutNotification.ENVOYEE);
                livraison = ResultatLivraisonEmail.ENVOYE;
                message = "Notification créée et e-mail envoyé à " + utilisateur.getEmail();
            } else {
                livraison = ResultatLivraisonEmail.DESACTIVE;
                message = "Notification créée — envoi e-mail désactivé (mode sans notification)";
            }
            notificationRepository.save(saved);
            integrationLogService.logEvent(
                    TypeLog.ENVOI_NOTIFICATION, StatutLog.SUCCES,
                    livraison == ResultatLivraisonEmail.ENVOYE
                            ? "E-mail de notification envoyé à " + utilisateur.getEmail()
                            : "Notification enregistrée sans envoi e-mail",
                    codingChallenge.getId()
            );
        } catch (RuntimeException exception) {
            saved.setStatut(StatutNotification.ECHEC);
            notificationRepository.save(saved);
            livraison = ResultatLivraisonEmail.ECHEC;
            message = "Notification créée mais échec d'envoi e-mail : " + exception.getMessage();
            integrationLogService.logEvent(
                    TypeLog.ENVOI_NOTIFICATION, StatutLog.ERREUR,
                    "Échec envoi notification à " + utilisateur.getEmail()
                            + " : " + exception.getMessage(),
                    codingChallenge.getId()
            );
        }
        return new NotificationCreationResult(
                enrichWithFeedback(mapper.toNotificationDto(saved)),
                false,
                livraison,
                actionUrl,
                message
        );
    }

    @Override
    public int relancerNotificationsNonLues() {
        var relance = notificationProperties.relance();
        if (relance == null || !relance.enabled()) {
            return 0;
        }
        ZonedDateTime seuil = ZonedDateTime.now().minus(relance.delay());
        List<Notification> due = notificationRepository.findDueForRelance(
                Set.of(
                        StatutNotification.EN_ATTENTE,
                        StatutNotification.ENVOYEE,
                        StatutNotification.ECHEC,
                        StatutNotification.LUE
                ),
                relance.max(),
                seuil,
                StatutFeedback.SOUMIS
        );
        if (due.isEmpty()) {
            log.debug(
                    "Relance cycle: none due (wait {} after send, max {} relances, no submitted feedback)",
                    relance.delay(),
                    relance.max()
            );
            return 0;
        }
        int sent = 0;
        for (Notification notification : due) {
            if (relancerUne(notification)) {
                sent++;
            }
        }
        log.info("Relance cycle: {} due, {} e-mail(s) sent", due.size(), sent);
        return sent;
    }

    private boolean relancerUne(Notification notification) {
        Utilisateur utilisateur = notification.getUtilisateur();
        CodingChallenge challenge = notification.getCodingChallenge();
        if (utilisateur == null || challenge == null) {
            return false;
        }
        rotateSetupToken(utilisateur);
        String actionUrl = buildActionUrl(utilisateur, challenge);
        try {
            emailSender.sendChallengeRelanceEmail(
                    utilisateur,
                    challenge,
                    actionUrl,
                    notification.getNombreRelances() + 1
            );
            notification.setNombreRelances(notification.getNombreRelances() + 1);
            notification.setDateDerniereRelance(ZonedDateTime.now());
            notification.setStatut(
                    notificationProperties.enabled()
                            ? StatutNotification.ENVOYEE
                            : StatutNotification.EN_ATTENTE
            );
            notificationRepository.save(notification);
            integrationLogService.logEvent(
                    TypeLog.RELANCE,
                    StatutLog.SUCCES,
                    "Relance #" + notification.getNombreRelances()
                            + " sent to " + utilisateur.getEmail()
                            + " for challenge " + challenge.getTitre(),
                    challenge.getId()
            );
            return true;
        } catch (RuntimeException exception) {
            notification.setStatut(StatutNotification.ECHEC);
            notificationRepository.save(notification);
            integrationLogService.logEvent(
                    TypeLog.RELANCE,
                    StatutLog.ERREUR,
                    "Relance failed for " + utilisateur.getEmail()
                            + ": " + exception.getMessage(),
                    challenge.getId()
            );
            return false;
        }
    }

    private void ensureSetupToken(Utilisateur utilisateur) {
        if (utilisateur.isCompteComplet()) {
            return;
        }
        if (!SetupTokenConstants.isExpiredOrMissing(
                utilisateur.getSetupToken(),
                utilisateur.getSetupTokenExpiresAt()
        )) {
            utilisateur.setSetupTokenExpiresAt(SetupTokenConstants.expiresAtFromNow());
            utilisateurRepository.save(utilisateur);
            return;
        }
        utilisateur.setSetupToken(UUID.randomUUID().toString());
        utilisateur.setSetupTokenExpiresAt(SetupTokenConstants.expiresAtFromNow());
        utilisateurRepository.save(utilisateur);
    }

    /** Relance only: invalidate previous setup links and issue a fresh token. */
    private void rotateSetupToken(Utilisateur utilisateur) {
        if (utilisateur.isCompteComplet()) {
            return;
        }
        utilisateur.setSetupToken(UUID.randomUUID().toString());
        utilisateur.setSetupTokenExpiresAt(SetupTokenConstants.expiresAtFromNow());
        utilisateurRepository.save(utilisateur);
    }

    private String resolveActionUrl(Utilisateur utilisateur, CodingChallenge codingChallenge) {
        ensureSetupToken(utilisateur);
        return buildActionUrl(utilisateur, codingChallenge);
    }

    private String buildActionUrl(Utilisateur utilisateur, CodingChallenge codingChallenge) {
        if (!utilisateur.isCompteComplet() && utilisateur.getSetupToken() != null) {
            return notificationProperties.frontendBaseUrl()
                    + "/complete-account?token="
                    + utilisateur.getSetupToken()
                    + "&challengeId="
                    + codingChallenge.getId();
        }
        return notificationProperties.frontendBaseUrl()
                + "/feedback/form?challengeId="
                + codingChallenge.getId();
    }
}