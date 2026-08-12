package com.stage.backend.email;

import com.stage.backend.config.notification.NotificationProperties;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Utilisateur;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailSender {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final NotificationProperties notificationProperties;

    public void sendChallengeCompletionEmail(
            Utilisateur user,
            CodingChallenge challenge,
            String actionUrl
    ) {
        if (!notificationProperties.enabled()) {
            log.info(
                    "Email delivery disabled. Notification persisted for user='{}' challenge='{}'",
                    user.getEmail(),
                    challenge.getTitre()
            );
            return;
        }

        String recipient = resolveRecipient(user);
        if (!StringUtils.hasText(recipient)) {
            log.warn(
                    "No email recipient for challenge='{}'. Skipping send.",
                    challenge.getTitre()
            );
            return;
        }

        JavaMailSender sender = requireMailSender();
        if (sender == null) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(notificationProperties.from());
        message.setTo(recipient);
        message.setSubject("CodePulse - Feedback requested for: " + challenge.getTitre());
        message.setText(buildEmailBody(user, challenge, actionUrl));

        sender.send(message);
        log.info(
                "Notification email sent to='{}' (challenge user='{}') challenge='{}'",
                recipient,
                user.getEmail(),
                challenge.getTitre()
        );
    }

    public void sendChallengeRelanceEmail(
            Utilisateur user,
            CodingChallenge challenge,
            String actionUrl,
            int relanceNumero
    ) {
        if (!notificationProperties.enabled()) {
            log.info(
                    "Email delivery disabled. Relance #{} stored for user='{}' challenge='{}'",
                    relanceNumero,
                    user.getEmail(),
                    challenge.getTitre()
            );
            return;
        }

        String recipient = resolveRecipient(user);
        if (!StringUtils.hasText(recipient)) {
            log.warn("No email recipient for relance challenge='{}'. Skipping send.", challenge.getTitre());
            return;
        }

        JavaMailSender sender = requireMailSender();
        if (sender == null) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(notificationProperties.from());
        message.setTo(recipient);
        message.setSubject("CodePulse - Reminder: feedback requested for: " + challenge.getTitre());
        message.setText(buildRelanceBody(user, challenge, actionUrl, relanceNumero));

        sender.send(message);
        log.info(
                "Relance #{} email sent to='{}' (challenge user='{}') challenge='{}'",
                relanceNumero,
                recipient,
                user.getEmail(),
                challenge.getTitre()
        );
    }

    public void sendPasswordResetLink(Utilisateur user, String actionUrl) {
        sendSimpleMessage(
                user,
                "CodePulse - Password reset",
                """
                Hello %s %s,

                An administrator approved your password reset request.

                Set a new password using the link below (valid for 24 hours):
                %s

                Thank you,
                CodePulse
                """.formatted(safe(user.getPrenom()), safe(user.getNom()), actionUrl)
        );
    }

    public void sendTemporaryPasswordEmail(Utilisateur user, String temporaryPassword) {
        sendSimpleMessage(
                user,
                "CodePulse - Temporary password",
                """
                Hello %s %s,

                An administrator set a temporary password for your CodePulse account.

                Temporary password: %s

                Sign in at CodePulse and change it from your profile when possible.

                Thank you,
                CodePulse
                """.formatted(safe(user.getPrenom()), safe(user.getNom()), temporaryPassword)
        );
    }

    private void sendSimpleMessage(Utilisateur user, String subject, String body) {
        if (!notificationProperties.enabled()) {
            log.info("Email delivery disabled. Skipping mail to='{}' subject='{}'", user.getEmail(), subject);
            return;
        }

        String recipient = resolveRecipient(user);
        if (!StringUtils.hasText(recipient)) {
            log.warn("No email recipient for subject='{}'. Skipping send.", subject);
            return;
        }

        JavaMailSender sender = requireMailSender();
        if (sender == null) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(notificationProperties.from());
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
        log.info("Email sent to='{}' subject='{}'", recipient, subject);
    }

    private JavaMailSender requireMailSender() {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Email enabled but JavaMailSender is not configured (no Mailpit/SMTP). Skipping send.");
        }
        return sender;
    }

    private String resolveRecipient(Utilisateur user) {
        if (StringUtils.hasText(notificationProperties.to())) {
            return notificationProperties.to().trim();
        }
        return user.getEmail();
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String buildEmailBody(Utilisateur user, CodingChallenge challenge, String actionUrl) {
        return """
                Hello %s %s,

                You have completed the coding challenge "%s".

                Please share your feedback by opening the link below:
                %s

                Thank you,
                CodePulse
                """.formatted(
                safe(user.getPrenom()),
                safe(user.getNom()),
                challenge.getTitre(),
                actionUrl
        );
    }

    private String buildRelanceBody(
            Utilisateur user,
            CodingChallenge challenge,
            String actionUrl,
            int relanceNumero
    ) {
        return """
                Hello %s %s,

                Reminder #%s: we have not received your feedback for the coding challenge "%s".

                Previous links are no longer valid. Please use this new link:
                %s

                Thank you,
                CodePulse
                """.formatted(
                safe(user.getPrenom()),
                safe(user.getNom()),
                relanceNumero,
                challenge.getTitre(),
                actionUrl
        );
    }
}
