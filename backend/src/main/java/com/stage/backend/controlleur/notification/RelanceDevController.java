package com.stage.backend.controlleur.notification;

import com.stage.backend.config.notification.NotificationProperties;
import com.stage.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Manual relance trigger for standalone demos (Gmail / GreenMail). */
@RestController
@Profile("standalone")
@RequestMapping("/dev/relance")
@RequiredArgsConstructor
public class RelanceDevController {

    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;

    @GetMapping("/run")
    public ResponseEntity<Map<String, Object>> runNow() {
        if (!isRelanceEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "sent", 0,
                            "enabled", false,
                            "message",
                            "Relances désactivées — définir codepulse.notification.relance.enabled=true "
                                    + "dans application.properties puis redémarrer le backend."
                    )
            );
        }
        int sent = notificationService.relancerNotificationsNonLues();
        return ResponseEntity.ok(
                Map.of(
                        "sent", sent,
                        "enabled", true,
                        "message",
                        sent > 0
                                ? sent + " relance(s) envoyée(s)."
                                : "Aucune relance due (attendre codepulse.notification.relance.delay après envoi)."
                )
        );
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        var relance = notificationProperties.relance();
        return ResponseEntity.ok(
                Map.of(
                        "enabled", isRelanceEnabled(),
                        "delay", relance == null ? "" : relance.delay().toString(),
                        "checkInterval", relance == null ? "" : relance.checkInterval().toString(),
                        "max", relance == null ? 0 : relance.max()
                )
        );
    }

    private boolean isRelanceEnabled() {
        var relance = notificationProperties.relance();
        return relance != null && relance.enabled();
    }
}
