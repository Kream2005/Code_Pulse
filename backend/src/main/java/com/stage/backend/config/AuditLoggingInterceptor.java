package com.stage.backend.config;

import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.security.JwtUtils;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private final IntegrationLogService integrationLogService;

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            return;
        }

        TypeLog type = resolveType(request.getMethod(), path);
        StatutLog statut = resolveStatut(response.getStatus(), ex);
        String actor = resolveActor();
        String message = request.getMethod() + " " + path
                + " → " + response.getStatus()
                + " by " + actor
                + (ex != null ? " (" + ex.getClass().getSimpleName() + ")" : "");

        try {
            integrationLogService.logEvent(type, statut, message, null);
        } catch (Exception ignored) {
            // never fail a business request because auditing failed
        }
    }

    private boolean shouldSkip(String path) {
        return path == null
                || path.startsWith("/integration-logs")
                || path.startsWith("/actuator")
                || path.startsWith("/error");
    }

    private TypeLog resolveType(String method, String path) {
        if (path.startsWith("/auth")) {
            return TypeLog.AUTH;
        }
        if (path.startsWith("/utilisateurs")) {
            return TypeLog.GESTION_UTILISATEUR;
        }
        if (path.startsWith("/coding-challenges")) {
            return TypeLog.GESTION_CHALLENGE;
        }
        if (path.startsWith("/notifications")) {
            return TypeLog.ENVOI_NOTIFICATION;
        }
        if (path.startsWith("/feedbacks")
                || path.startsWith("/questions-feedback")
                || path.startsWith("/reponses-feedback")) {
            return TypeLog.FEEDBACK;
        }
        if (path.startsWith("/demandes-reinit")) {
            return TypeLog.DEMANDE_REINIT;
        }
        if (path.startsWith("/analytics")) {
            return TypeLog.EXPORT_DONNEES;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return TypeLog.CONSULTATION;
        }
        return TypeLog.SYSTEME;
    }

    private StatutLog resolveStatut(int status, Exception ex) {
        if (ex != null || status >= 500) {
            return StatutLog.ERREUR;
        }
        if (status == 401 || status == 403) {
            return StatutLog.WARNING;
        }
        if (status >= 400) {
            return StatutLog.WARNING;
        }
        return StatutLog.INFO;
    }

    private String resolveActor() {
        try {
            Long uid = JwtUtils.getCurrentUserId();
            return uid != null ? "uid=" + uid : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
