package com.stage.backend.controlleur.notification;

import com.stage.backend.dto.notification.CreateNotificationRequest;
import com.stage.backend.dto.notification.NotificationDto;
import com.stage.backend.dto.notification.NotificationEnvoiResponse;
import com.stage.backend.dto.notification.NotificationStatutUpdateResponse;
import com.stage.backend.enums.StatutNotification;
import com.stage.backend.security.JwtUtils;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.notification.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService service;

    @PostMapping
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<NotificationEnvoiResponse> envoyerNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) {
        return ResponseEntity.ok(service.envoyerNotification(request));
    }

    @GetMapping("/get-notification/{id}")
    @PreAuthorize(SecurityRoles.USER_OR_READ_FEEDBACKS)
    public ResponseEntity<NotificationDto> getNotification(
            @PathVariable Long id
    ) {
        NotificationDto notification = service.getNotification(id);
        assertNotificationAccess(notification.utilisateurId());
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/get-notification-by-utilisateur")
    @PreAuthorize(SecurityRoles.USER_OR_READ_FEEDBACKS)
    public ResponseEntity<List<NotificationDto>> getNotificationsByUtilisateur(
            @RequestParam Long utilisateurId
    ) {
        assertNotificationAccess(utilisateurId);
        return ResponseEntity.ok(service.getNotificationsByUtilisateur(utilisateurId));
    }

    @GetMapping("/get-notifications-by-utilisateur-pages/page")
    @PreAuthorize(SecurityRoles.USER_OR_READ_FEEDBACKS)
    public ResponseEntity<Page<NotificationDto>> getNotificationsByUtilisateurPage(
            @RequestParam Long utilisateurId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StatutNotification statut,
            @RequestParam(required = false) String tag
    ) {
        assertNotificationAccess(utilisateurId);
        return ResponseEntity.ok(service.searchNotificationsByUtilisateur(utilisateurId, q, statut, tag, page, size));
    }

    @GetMapping("/get-all-notifications")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<List<NotificationDto>> getAllNotifications() {
        return ResponseEntity.ok(service.getAllNotifications());
    }

    @GetMapping("/get-notifications-pages/page")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<Page<NotificationDto>> getAllNotificationsPage(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) StatutNotification statut,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tag
    ) {
        return ResponseEntity.ok(service.searchNotifications(q, statut, tag, page, size));
    }

    @GetMapping("/get-notification-by-statut")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<List<NotificationDto>> getNotificationsByStatut(
            @RequestParam StatutNotification statut
    ) {
        return ResponseEntity.ok(service.getNotificationsByStatut(statut));
    }

    @PatchMapping("/update-statut/{id}/statut")
    @PreAuthorize(SecurityRoles.USER_OR_READ_FEEDBACKS)
    public ResponseEntity<NotificationStatutUpdateResponse> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutNotification statut
    ) {
        NotificationDto notification = service.getNotification(id);
        assertNotificationAccess(notification.utilisateurId());
        return ResponseEntity.ok(service.changerStatut(id, statut));
    }

    @GetMapping("/count")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<Long> countNotifications() {
        return ResponseEntity.ok(service.countNotifications());
    }

    @GetMapping("/count/statut")
    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    public ResponseEntity<Long> countNotificationsByStatut(
            @RequestParam StatutNotification statut
    ) {
        return ResponseEntity.ok(service.countNotificationsByStatut(statut));
    }

    private void assertNotificationAccess(Long utilisateurId) {
        Long currentUserId = JwtUtils.getCurrentUserId();
        if (utilisateurId.equals(currentUserId)) {
            return;
        }
        boolean admin = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_CODING_CHALLENGE")
                        || a.getAuthority().equals("ROLE_MANAGER_RH")
                        || a.getAuthority().equals("ROLE_ADMIN_CODEPULSE"));
        if (!admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
    }
}
