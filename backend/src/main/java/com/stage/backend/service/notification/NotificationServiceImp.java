package com.stage.backend.service.notification;

import com.stage.backend.config.notification.NotificationProperties;
import com.stage.backend.dto.notification.CreateNotificationRequest;
import com.stage.backend.dto.notification.NotificationDto;
import com.stage.backend.email.NotificationEmailSender;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Notification;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.StatutNotification;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.mapper.NotificationMapper;
import com.stage.backend.repository.CodingChallengeRepository;
import com.stage.backend.repository.NotificationRepository;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.service.integrationlog.IntegrationLogService;
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

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationServiceImp implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final CodingChallengeRepository codingChallengeRepository;
    private final NotificationMapper mapper;
    private final NotificationEmailSender emailSender;
    private final NotificationProperties notificationProperties;
    private final IntegrationLogService integrationLogService;

    @Override
    public NotificationDto envoyerNotification(CreateNotificationRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(request.utilisateurId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User with id: " + request.utilisateurId() + " was not found"
                ));

        CodingChallenge codingChallenge = codingChallengeRepository.findById(request.codingChallengeId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Coding challenge with id: " + request.codingChallengeId() + " was not found"
                ));

        return notifyChallengeCompletion(utilisateur, codingChallenge);
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

        return mapper.toNotificationDto(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByUtilisateur(Long utilisateurId) {
        return notificationRepository.findByUtilisateurId(utilisateurId)
                .stream()
                .map(mapper::toNotificationDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByUtilisateurPage(Long utilisateurId, int page, int size) {
        return notificationRepository
                .findByUtilisateurIdAndSupprimeFalse(utilisateurId, PageRequest.of(page, size))
                .map(mapper::toNotificationDto);
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
                .map(mapper::toNotificationDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getAllNotifications() {
        return notificationRepository.findAllWithDetails()
                .stream()
                .map(mapper::toNotificationDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getAllNotificationsPage(int page, int size) {
        return notificationRepository.findAllBy(PageRequest.of(page, size))
                .map(mapper::toNotificationDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getNotificationsByStatut(StatutNotification statut) {
        return notificationRepository.findByStatut(statut)
                .stream()
                .map(mapper::toNotificationDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotificationsByStatutPage(StatutNotification statut, int page, int size) {
        return notificationRepository.findByStatut(statut, PageRequest.of(page, size))
                .map(mapper::toNotificationDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDto> searchNotifications(
            String keyword, StatutNotification statut, String tag, int page, int size
    ) {
        String normalized = keyword == null ? "" : keyword.trim();
        String normalizedTag = tag == null ? "" : tag.trim();
        return notificationRepository.search(normalized, statut, normalizedTag, PageRequest.of(page, size))
                .map(mapper::toNotificationDto);
    }

    @Override
    public boolean changerStatut(Long notificationId, StatutNotification statut) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Notification with id: "
                                        + notificationId
                                        + " was not found"
                        )
                );

        notification.setStatut(statut);

        notificationRepository.save(notification);

        integrationLogService.logEvent(
                TypeLog.ENVOI_NOTIFICATION,
                StatutLog.SUCCES,
                "Notification " + notificationId + " status changed to " + statut,
                notification.getCodingChallenge() != null ? notification.getCodingChallenge().getId() : null
        );

        return true;
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
    public NotificationDto notifyChallengeCompletion(Utilisateur utilisateur, CodingChallenge codingChallenge) {
        Optional<Notification> existing = notificationRepository.findByUtilisateurIdAndCodingChallengeId(
                utilisateur.getId(),
                codingChallenge.getId()
        );
        if (existing.isPresent()) {
            integrationLogService.logEvent(
                    TypeLog.ENVOI_NOTIFICATION, StatutLog.INFO,
                    "Notification already exists for user " + utilisateur.getEmail()
                            + " and challenge " + codingChallenge.getTitre(),
                    codingChallenge.getId()
            );
            return mapper.toNotificationDto(existing.get());
        }
        Notification notification = new Notification();
        notification.setUtilisateur(utilisateur);
        notification.setCodingChallenge(codingChallenge);
        notification.setDateEnvoi(ZonedDateTime.now());
        notification.setStatut(StatutNotification.EN_ATTENTE);
        Notification saved = notificationRepository.save(notification);
        integrationLogService.logEvent(
                TypeLog.ENVOI_NOTIFICATION, StatutLog.INFO,
                "Notification created for user " + utilisateur.getEmail()
                        + " and challenge " + codingChallenge.getTitre(),
                codingChallenge.getId()
        );
        try {
            emailSender.sendChallengeCompletionEmail(
                    utilisateur,
                    codingChallenge,
                    buildActionUrl(utilisateur, codingChallenge)
            );
            saved.setStatut(
                    notificationProperties.enabled()
                            ? StatutNotification.ENVOYEE
                            : StatutNotification.EN_ATTENTE
            );
            notificationRepository.save(saved);
            integrationLogService.logEvent(
                    TypeLog.ENVOI_NOTIFICATION, StatutLog.SUCCES,
                    notificationProperties.enabled()
                            ? "Notification email sent to " + utilisateur.getEmail()
                            : "Notification stored without email delivery (email disabled)",
                    codingChallenge.getId()
            );
        } catch (RuntimeException exception) {
            saved.setStatut(StatutNotification.ECHEC);
            notificationRepository.save(saved);
            integrationLogService.logEvent(
                    TypeLog.ENVOI_NOTIFICATION, StatutLog.ERREUR,
                    "Failed to send notification to " + utilisateur.getEmail()
                            + ": " + exception.getMessage(),
                    codingChallenge.getId()
            );
        }
        return mapper.toNotificationDto(saved);
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