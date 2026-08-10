package com.stage.backend.config;

import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Hibernate ddl-auto=update does not refresh PostgreSQL enum CHECK constraints
 * when Java enums gain new values (e.g. TypeLog.CONFIG). Align them at startup.
 */
@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class IntegrationLogSchemaAligner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            alignCheck("integration_log_type_check", "type", enumLiterals(TypeLog.values()));
            alignCheck("integration_log_statut_check", "statut", enumLiterals(StatutLog.values()));
        } catch (Exception ex) {
            log.warn("Could not align integration_log check constraints: {}", ex.getMessage());
        }
    }

    private void alignCheck(String constraintName, String column, String literals) {
        jdbcTemplate.execute(
                "ALTER TABLE integration_log DROP CONSTRAINT IF EXISTS " + constraintName
        );
        jdbcTemplate.execute(
                "ALTER TABLE integration_log ADD CONSTRAINT " + constraintName
                        + " CHECK (" + column + " IS NULL OR " + column + " IN (" + literals + "))"
        );
        log.info("Aligned integration_log.{} check constraint ({})", column, constraintName);
    }

    private static String enumLiterals(Enum<?>[] values) {
        return Arrays.stream(values)
                .map(Enum::name)
                .map(name -> "'" + name + "'")
                .collect(Collectors.joining(", "));
    }
}
