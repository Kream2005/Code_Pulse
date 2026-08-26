package com.stage.backend.email;

import com.stage.backend.config.notification.NotificationProperties;
import com.stage.backend.entity.CodingChallenge;
import com.stage.backend.entity.Utilisateur;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailSender {

    private static final String BRAND = "#0070ad";

    private final ObjectProvider<JavaMailSender> mailSender;
    private final NotificationProperties notificationProperties;

    public void sendChallengeCompletionEmail(
            Utilisateur user,
            CodingChallenge challenge,
            String actionUrl
    ) {
        String titre = safe(challenge.getTitre());
        String subject = "CodePulse — Votre avis sur « " + titre + " »";
        String buttonLabel = user.isCompteComplet()
                ? "Donner mon feedback"
                : "Activer mon compte et donner mon feedback";
        String intro = user.isCompteComplet()
                ? "Vous avez terminé le coding challenge <strong>"
                        + HtmlUtils.htmlEscape(titre)
                        + "</strong>."
                : "Un coding challenge vous attend sur CodePulse : <strong>"
                        + HtmlUtils.htmlEscape(titre)
                        + "</strong>. Activez d’abord votre compte, puis partagez votre avis.";
        String bodyText =
                "Bonjour "
                        + displayName(user)
                        + ",\n\n"
                        + "Vous avez terminé le coding challenge \""
                        + titre
                        + "\".\n\n"
                        + "Merci de partager votre feedback via ce lien :\n"
                        + actionUrl
                        + "\n\n"
                        + "Cordialement,\nL’équipe CodePulse\n";
        String bodyHtml = htmlDocument(
                "Feedback demandé",
                greetingHtml(user),
                "<p>" + intro + "</p>"
                        + "<p>Merci de prendre quelques minutes pour nous faire part de votre retour.</p>",
                actionUrl,
                buttonLabel,
                "Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>"
                        + linkFallback(actionUrl)
        );
        sendHtmlMessage(user, subject, bodyText, bodyHtml);
    }

    public void sendChallengeRelanceEmail(
            Utilisateur user,
            CodingChallenge challenge,
            String actionUrl,
            int relanceNumero
    ) {
        String titre = safe(challenge.getTitre());
        String subject = "CodePulse — Rappel #" + relanceNumero + " : feedback pour « " + titre + " »";
        String bodyText =
                "Bonjour "
                        + displayName(user)
                        + ",\n\n"
                        + "Rappel n°"
                        + relanceNumero
                        + " : nous n’avons pas encore reçu votre feedback pour le challenge \""
                        + titre
                        + "\".\n\n"
                        + "Les liens précédents ne sont plus valides. Utilisez ce nouveau lien :\n"
                        + actionUrl
                        + "\n\n"
                        + "Cordialement,\nL’équipe CodePulse\n";
        String bodyHtml = htmlDocument(
                "Rappel de feedback",
                greetingHtml(user),
                "<p>Rappel n°"
                        + relanceNumero
                        + " : nous n’avons pas encore reçu votre feedback pour le challenge "
                        + "<strong>"
                        + HtmlUtils.htmlEscape(titre)
                        + "</strong>.</p>"
                        + "<p>Les liens précédents ne sont plus valides. Merci d’utiliser le bouton ci-dessous.</p>",
                actionUrl,
                "Ouvrir le formulaire de feedback",
                "Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>"
                        + linkFallback(actionUrl)
        );
        sendHtmlMessage(user, subject, bodyText, bodyHtml);
    }

    public void sendPasswordResetLink(Utilisateur user, String actionUrl) {
        String subject = "CodePulse — Réinitialisation de votre mot de passe";
        String bodyText =
                "Bonjour "
                        + displayName(user)
                        + ",\n\n"
                        + "Un administrateur a validé votre demande de réinitialisation de mot de passe.\n\n"
                        + "Définissez un nouveau mot de passe (lien valable 24 h) :\n"
                        + actionUrl
                        + "\n\n"
                        + "Cordialement,\nL’équipe CodePulse\n";
        String bodyHtml = htmlDocument(
                "Réinitialisation du mot de passe",
                greetingHtml(user),
                "<p>Un administrateur a validé votre demande de réinitialisation de mot de passe.</p>"
                        + "<p>Cliquez sur le bouton ci-dessous pour en définir un nouveau "
                        + "(lien valable <strong>24 heures</strong>).</p>",
                actionUrl,
                "Réinitialiser mon mot de passe",
                "Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>"
                        + linkFallback(actionUrl)
        );
        sendHtmlMessage(user, subject, bodyText, bodyHtml);
    }

    public void sendTemporaryPasswordEmail(Utilisateur user, String temporaryPassword) {
        String subject = "CodePulse — Mot de passe temporaire";
        String bodyText =
                "Bonjour "
                        + displayName(user)
                        + ",\n\n"
                        + "Un administrateur a défini un mot de passe temporaire pour votre compte CodePulse.\n\n"
                        + "Mot de passe temporaire : "
                        + temporaryPassword
                        + "\n\n"
                        + "Connectez-vous puis changez-le depuis votre profil dès que possible.\n\n"
                        + "Cordialement,\nL’équipe CodePulse\n";
        String bodyHtml = htmlDocument(
                "Mot de passe temporaire",
                greetingHtml(user),
                "<p>Un administrateur a défini un mot de passe temporaire pour votre compte CodePulse.</p>"
                        + "<p style=\"margin:20px 0;padding:14px 18px;background:#f1f5f9;border-radius:8px;"
                        + "font-family:ui-monospace,monospace;font-size:16px;letter-spacing:0.04em;\">"
                        + HtmlUtils.htmlEscape(temporaryPassword)
                        + "</p>"
                        + "<p>Connectez-vous à CodePulse, puis changez-le depuis votre profil dès que possible.</p>",
                null,
                null,
                null
        );
        sendHtmlMessage(user, subject, bodyText, bodyHtml);
    }

    public void sendDevTestEmail() {
        Utilisateur probe = new Utilisateur();
        probe.setEmail("demo.user@codepulse.local");
        probe.setPrenom("Demo");
        probe.setNom("User");
        String subject = "CodePulse — E-mail de test";
        String bodyText =
                "Bonjour Demo User,\n\n"
                        + "Si ce message apparaît dans /dev/mailbox, le SMTP local fonctionne.\n\n"
                        + "Cordialement,\nL’équipe CodePulse\n";
        String bodyHtml = htmlDocument(
                "E-mail de test",
                "<p>Bonjour <strong>Demo User</strong>,</p>",
                "<p>Si ce message apparaît dans <code>/dev/mailbox</code>, le SMTP local fonctionne.</p>",
                notificationProperties.frontendBaseUrl(),
                "Ouvrir CodePulse",
                null
        );
        sendHtmlMessage(probe, subject, bodyText, bodyHtml);
    }

    private void sendHtmlMessage(Utilisateur user, String subject, String plainText, String htmlBody) {
        if (!notificationProperties.enabled()) {
            log.info(
                    "Envoi e-mail désactivé. Destinataire prévu='{}' sujet='{}'",
                    user.getEmail(),
                    subject
            );
            return;
        }

        String recipient = resolveRecipient(user);
        if (!StringUtils.hasText(recipient)) {
            log.warn("Aucun destinataire pour le sujet='{}'. Envoi ignoré.", subject);
            return;
        }

        JavaMailSender sender = requireMailSender();
        if (sender == null) {
            return;
        }

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(notificationProperties.from());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(plainText, htmlBody);
            sender.send(message);
            log.info("E-mail HTML envoyé à='{}' sujet='{}'", recipient, subject);
        } catch (MessagingException exception) {
            throw new IllegalStateException(
                    "Échec d’envoi de l’e-mail à " + recipient + " : " + exception.getMessage(),
                    exception
            );
        }
    }

    private JavaMailSender requireMailSender() {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn(
                    "E-mails activés mais JavaMailSender non configuré (SMTP manquant). Envoi ignoré."
            );
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

    private String displayName(Utilisateur user) {
        String prenom = safe(user.getPrenom()).trim();
        String nom = safe(user.getNom()).trim();
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? "utilisateur" : full;
    }

    private String greetingHtml(Utilisateur user) {
        return "<p>Bonjour <strong>" + HtmlUtils.htmlEscape(displayName(user)) + "</strong>,</p>";
    }

    private String linkFallback(String url) {
        String safeUrl = HtmlUtils.htmlEscape(url);
        return "<a href=\""
                + safeUrl
                + "\" style=\"color:"
                + BRAND
                + ";word-break:break-all;\">"
                + safeUrl
                + "</a>";
    }

    private String ctaButton(String url, String label) {
        String safeUrl = HtmlUtils.htmlEscape(url);
        String safeLabel = HtmlUtils.htmlEscape(label);
        // Table + bgcolor + padding: looks like a real button in Gmail / Outlook / Apple Mail.
        return """
                <table role="presentation" cellspacing="0" cellpadding="0" border="0" align="center" style="margin:28px auto;">
                  <tr>
                    <td align="center" bgcolor="%s" style="background-color:%s;border-radius:8px;mso-padding-alt:14px 28px;">
                      <!--[if mso]>
                      <v:roundrect xmlns:v="urn:schemas-microsoft-com:vml" xmlns:w="urn:schemas-microsoft-com:office:word"
                        href="%s" style="height:48px;v-text-anchor:middle;width:280px;" arcsize="12%%" stroke="f" fillcolor="%s">
                        <w:anchorlock/>
                        <center style="color:#ffffff;font-family:Arial,sans-serif;font-size:16px;font-weight:bold;">%s</center>
                      </v:roundrect>
                      <![endif]-->
                      <!--[if !mso]><!-- -->
                      <a href="%s"
                         target="_blank"
                         style="display:inline-block;padding:14px 28px;font-family:Arial,Helvetica,sans-serif;
                                font-size:16px;line-height:1.25;font-weight:700;color:#ffffff !important;
                                text-decoration:none;border-radius:8px;background-color:%s;
                                border:1px solid %s;mso-hide:all;">
                        %s
                      </a>
                      <!--<![endif]-->
                    </td>
                  </tr>
                </table>
                """.formatted(
                BRAND, BRAND, safeUrl, BRAND, safeLabel, safeUrl, BRAND, BRAND, safeLabel
        );
    }

    private String htmlDocument(
            String eyebrow,
            String greetingHtml,
            String paragraphsHtml,
            String actionUrl,
            String buttonLabel,
            String footerExtraHtml
    ) {
        String buttonBlock = "";
        if (StringUtils.hasText(actionUrl) && StringUtils.hasText(buttonLabel)) {
            buttonBlock = ctaButton(actionUrl, buttonLabel);
        }
        String extra = footerExtraHtml != null ? "<p style=\"font-size:13px;color:#64748b;\">" + footerExtraHtml + "</p>" : "";
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#f1f5f9;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background:#f1f5f9;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                               style="max-width:560px;background:#ffffff;border-radius:12px;overflow:hidden;
                                      border:1px solid #e2e8f0;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                          <tr>
                            <td style="background:%s;padding:18px 24px;">
                              <div style="font-size:20px;font-weight:700;color:#ffffff;">CodePulse</div>
                              <div style="font-size:13px;color:#dbeafe;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px 24px 8px 24px;font-size:15px;line-height:1.55;">
                              %s
                              %s
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:8px 24px 28px 24px;">
                              %s
                              <p style="margin:24px 0 0 0;font-size:14px;color:#334155;">
                                Cordialement,<br><strong>L’équipe CodePulse</strong>
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(eyebrow),
                BRAND,
                HtmlUtils.htmlEscape(eyebrow),
                greetingHtml,
                paragraphsHtml,
                buttonBlock,
                extra
        );
    }
}
