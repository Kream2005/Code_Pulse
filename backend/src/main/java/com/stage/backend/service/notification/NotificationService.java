package com.stage.backend.service.notification;

import com.stage.backend.dto.notification.CreateNotificationRequest;
import com.stage.backend.dto.notification.NotificationCreationResult;
import com.stage.backend.dto.notification.NotificationDto;
import com.stage.backend.dto.notification.NotificationEnvoiResponse;
import com.stage.backend.dto.notification.NotificationStatutUpdateResponse;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.StatutNotification;
import org.springframework.data.domain.Page;

import java.util.List;

public interface NotificationService {

    NotificationEnvoiResponse envoyerNotification(CreateNotificationRequest request);

    NotificationDto getNotification(Long notificationId);

    List<NotificationDto> getNotificationsByUtilisateur(Long utilisateurId);

    Page<NotificationDto> getNotificationsByUtilisateurPage(Long utilisateurId, int page, int size);

    Page<NotificationDto> searchNotificationsByUtilisateur(
            Long utilisateurId, String keyword, StatutNotification statut, String tag, int page, int size
    );

    List<NotificationDto> getAllNotifications();

    Page<NotificationDto> getAllNotificationsPage(int page, int size);

    List<NotificationDto> getNotificationsByStatut(StatutNotification statut);

    Page<NotificationDto> getNotificationsByStatutPage(StatutNotification statut, int page, int size);

    Page<NotificationDto> searchNotifications(
            String keyword, StatutNotification statut, String tag, int page, int size
    );

    NotificationStatutUpdateResponse changerStatut(Long notificationId, StatutNotification statut);

    long countNotifications();

    long countNotificationsByStatut(StatutNotification statut);

    NotificationCreationResult notifyChallengeCompletion(Utilisateur utilisateur, CodingChallenge codingChallenge);

    int relancerNotificationsNonLues();
}
