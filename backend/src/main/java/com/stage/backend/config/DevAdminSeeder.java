package com.stage.backend.config;

import com.stage.backend.entity.Utilisateur;
import com.stage.backend.enums.Role;
import com.stage.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("full")
@RequiredArgsConstructor
@Slf4j
public class DevAdminSeeder implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        String email = "admin@codepulse.local";
        if (utilisateurRepository.existsByEmail(email)) {
            return;
        }

        Utilisateur admin = new Utilisateur();
        admin.setNom("Admin");
        admin.setPrenom("CodePulse");
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode("Admin1234!"));
        admin.setRole(Role.ADMIN_CODEPULSE);
        admin.setCompteComplet(true);

        utilisateurRepository.save(admin);
        log.info("Dev admin account created: {}", email);
    }
}
