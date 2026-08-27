package com.stage.backend.controlleur.notification;

import com.stage.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
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

    @GetMapping("/run")
    public ResponseEntity<Map<String, Object>> runNow() {
        int sent = notificationService.relancerNotificationsNonLues();
        return ResponseEntity.ok(
                Map.of(
                        "sent", sent,
                        "message",
                        sent > 0
                                ? sent + " relance(s) envoyée(s)."
                                : "Aucune relance due (attendre le délai configuré ou vérifier qu’aucun feedback n’est soumis)."
                )
        );
    }
}
