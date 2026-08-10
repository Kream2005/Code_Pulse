package com.stage.backend.config;

import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.Role;
import com.stage.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class RoleAccountsSeeder implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        ensure("demo.user@codepulse.local", "Demo", "User", "demo.user", "Demo1234!", Role.USER, 90002L);
        ensure(
                "challenge.admin@codepulse.local",
                "Challenge",
                "Admin",
                "challenge.admin",
                "Challenge1234!",
                Role.ADMIN_CODING_CHALLENGE,
                90003L
        );
        ensure(
                "manager.rh@codepulse.local",
                "Manager",
                "RH",
                "manager.rh",
                "Manager1234!",
                Role.MANAGER_RH,
                90004L
        );
        ensure(
                "admin@codepulse.local",
                "Admin",
                "CodePulse",
                "admin.codepulse",
                "Admin1234!",
                Role.ADMIN_CODEPULSE,
                90001L
        );
    }

    private void ensure(
            String email,
            String prenom,
            String nom,
            String username,
            String rawPassword,
            Role role,
            Long externalId
    ) {
        if (utilisateurRepository.existsByEmail(email)) {
            return;
        }
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setPrenom(prenom);
        u.setNom(nom);
        u.setUserName(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setRole(role);
        u.setCompteComplet(true);
        u.setStatus(true);
        u.setExternalId(externalId);
        utilisateurRepository.save(u);
        log.info("Role account created: {} ({})", email, role);
    }
}
