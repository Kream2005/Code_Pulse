package com.stage.backend.config;

import com.stage.backend.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class DemoUserEmailConfigurer implements ApplicationRunner {

    private final UtilisateurRepository utilisateurRepository;

    @Value("${codepulse.demo.user-email:}")
    private String demoUserEmail;

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(demoUserEmail)) {
            return;
        }
        utilisateurRepository.findByUserName("demo.user").ifPresent(user -> {
            String target = demoUserEmail.trim();
            if (target.equalsIgnoreCase(user.getEmail())) {
                return;
            }
            var holder = utilisateurRepository.findByEmailIgnoreCase(target);
            if (holder.isPresent() && !holder.get().getId().equals(user.getId())) {
                log.warn(
                        "codepulse.demo.user-email={} already used by user id={} — demo.user kept as {}",
                        target,
                        holder.get().getId(),
                        user.getEmail()
                );
                return;
            }
            String previous = user.getEmail();
            user.setEmail(target);
            utilisateurRepository.save(user);
            log.info("Demo user email updated: {} -> {}", previous, target);
        });
    }
}
