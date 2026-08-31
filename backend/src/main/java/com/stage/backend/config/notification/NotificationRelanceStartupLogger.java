package com.stage.backend.config.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationRelanceStartupLogger {

    private final NotificationProperties notificationProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logRelanceConfig() {
        var relance = notificationProperties.relance();
        if (relance == null || !relance.enabled()) {
            log.info(
                    "Notification relances: DISABLED (codepulse.notification.relance.enabled=false)"
            );
            return;
        }
        log.info(
                "Notification relances: ENABLED — delay={}, checkInterval={}, max={}",
                relance.delay(),
                relance.checkInterval(),
                relance.max()
        );
    }
}
