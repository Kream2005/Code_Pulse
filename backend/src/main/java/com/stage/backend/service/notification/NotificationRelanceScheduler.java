package com.stage.backend.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "codepulse.notification.relance.enabled", havingValue = "true")
public class NotificationRelanceScheduler {

    private final NotificationService notificationService;

    @Scheduled(
            initialDelayString = "${codepulse.notification.relance.initial-delay:45s}",
            fixedDelayString = "${codepulse.notification.relance.check-interval:15m}"
    )
    public void relancer() {
        try {
            notificationService.relancerNotificationsNonLues();
        } catch (RuntimeException exception) {
            log.warn("Notification relance cycle failed: {}", exception.getMessage());
        }
    }
}
