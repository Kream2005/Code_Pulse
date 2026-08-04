package com.stage.backend.controlleur.auth;

import com.stage.backend.dto.utilisateur.ProfileResponse;
import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.StatutLog;
import com.stage.backend.enums.TypeLog;
import com.stage.backend.repository.UtilisateurRepository;
import com.stage.backend.security.JwtUtils;
import com.stage.backend.security.SecurityRoles;
import com.stage.backend.service.integrationlog.IntegrationLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SecureController {

    private final UtilisateurRepository utilisateurRepository;
    private final IntegrationLogService integrationLogService;

    @GetMapping("/api/me")
    @PreAuthorize(SecurityRoles.AUTHENTICATED)
    public Map<String, Object> me() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Map<String, Object> body = new HashMap<>();
        body.put("subject", jwt.getSubject());
        body.put("issuer", jwt.getClaimAsString("iss"));

        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim instanceof List<?> list) {
            body.put("roles", list.stream().map(String::valueOf).toList());
        } else if (rolesClaim != null) {
            body.put("roles", List.of(String.valueOf(rolesClaim)));
        } else {
            body.put("roles", List.of());
        }

        Object uid = jwt.getClaim("uid");
        body.put("uid", uid instanceof Number number ? number.longValue() : uid);
        body.put("expiresAt", jwt.getExpiresAt() != null ? jwt.getExpiresAt().toString() : null);
        return body;
    }

    @GetMapping("/api/profile")
    @PreAuthorize(SecurityRoles.AUTHENTICATED)
    public ProfileResponse profile() {
        Long userId = JwtUtils.getCurrentUserId();
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        integrationLogService.logEvent(
                TypeLog.CONSULTATION,
                StatutLog.INFO,
                "Profile consulted by user " + user.getEmail(),
                null
        );
        return new ProfileResponse(user.getEmail(), user.getNom(), user.getPrenom(), user.getUserName());
    }

    @PreAuthorize(SecurityRoles.ADMIN_CHALLENGE)
    @GetMapping("/api/admin")
    public Map<String, String> adminOnly() {
        return Map.of("message", "admin access granted");
    }
}
