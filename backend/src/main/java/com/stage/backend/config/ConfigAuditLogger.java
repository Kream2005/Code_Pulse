package com.stage.backend.config;

import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ConfigAuditLogger implements ApplicationRunner {

    private final IntegrationLogService integrationLogService;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String profiles = String.join(",", environment.getActiveProfiles());
            if (profiles.isBlank()) {
                profiles = "default";
            }

            String mode = environment.getProperty("codepulse.mode", "standalone");
            boolean kafka = flag("codepulse.kafka.enabled");
            boolean notification = flag("codepulse.notification.enabled");
            boolean externalApi = flag("codepulse.external-api.enabled");

            logFlag("Kafka consumer/producer", kafka);
            logFlag("Notification emails", notification);
            logFlag("External coding-challenge API", externalApi);

            integrationLogService.logEvent(
                    TypeLog.CONFIG,
                    StatutLog.INFO,
                    "Application started — mode=" + mode
                            + " profiles=[" + profiles + "]"
                            + " kafka.enabled=" + kafka
                            + " notification.enabled=" + notification
                            + " external-api.enabled=" + externalApi,
                    null
            );
            log.info("Config audit recorded — mode={} profiles={}", mode, profiles);
        } catch (Exception ex) {
            // Never block boot on audit logging (e.g. stale DB check constraints).
            log.warn("Config audit skipped: {}", ex.getMessage());
        }
    }

    private boolean flag(String key) {
        return environment.getProperty(key, Boolean.class, false);
    }

    private void logFlag(String name, boolean enabled) {
        integrationLogService.logEvent(
                TypeLog.CONFIG,
                StatutLog.INFO,
                name + " switch is " + (enabled ? "ENABLED" : "DISABLED"),
                null
        );
    }
}
