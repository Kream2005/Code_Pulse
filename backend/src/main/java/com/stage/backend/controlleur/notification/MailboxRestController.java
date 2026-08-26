package com.stage.backend.controlleur.notification;

import com.icegreen.greenmail.util.GreenMail;
import com.stage.backend.dto.notification.CapturedMailDto;
import com.stage.backend.email.NotificationEmailSender;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@Profile("standalone")
@RequestMapping("/dev/mailbox")
@RequiredArgsConstructor
public class MailboxRestController {

    private static final int MAX_MESSAGES = 50;

    private final ObjectProvider<GreenMail> greenMail;
    private final NotificationEmailSender emailSender;

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public String page() {
        List<CapturedMailDto> mails = captured();
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="fr"><head><meta charset="utf-8"><title>Boîte mail CodePulse</title>
                <style>
                  body { font-family: Arial, Helvetica, sans-serif; max-width: 920px; margin: 2rem auto; color: #1e293b; }
                  a { color: #0070ad; }
                  article { border: 1px solid #e2e8f0; border-radius: 8px; padding: 1rem 1.2rem; margin: 1rem 0; background: #fff; }
                  .meta { font-size: 13px; color: #64748b; margin-bottom: 0.8rem; }
                  .preview { border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; background: #f8fafc; }
                </style></head><body>
                <h1>Boîte mail de démo</h1>
                <p>SMTP local GreenMail (pas d’envoi Internet).
                <a href="/dev/mailbox/send-test">Envoyer un e-mail de test</a> ·
                <a href="/dev/mailbox">Actualiser</a></p>
                """);
        if (mails.isEmpty()) {
            html.append("<p>Aucun message pour l’instant. Envoyez un test, ou déclenchez une notification / relance.</p>");
        }
        for (CapturedMailDto mail : mails) {
            html.append("<article><div class=\"meta\"><b>")
                    .append(esc(mail.subject()))
                    .append("</b><br>À : ")
                    .append(esc(mail.to()))
                    .append("<br>")
                    .append(esc(String.valueOf(mail.sentAt())))
                    .append("</div><div class=\"preview\">")
                    .append(mail.body())
                    .append("</div></article>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CapturedMailDto>> json() {
        return ResponseEntity.ok(captured());
    }

    @GetMapping("/send-test")
    public ResponseEntity<Void> sendTest() {
        emailSender.sendDevTestEmail();
        return ResponseEntity.status(302).header("Location", "/dev/mailbox").build();
    }

    private List<CapturedMailDto> captured() {
        GreenMail mailbox = greenMail.getIfAvailable();
        if (mailbox == null) {
            return List.of();
        }
        MimeMessage[] received = mailbox.getReceivedMessages();
        int from = Math.max(0, received.length - MAX_MESSAGES);
        List<CapturedMailDto> mails = new ArrayList<>();
        for (int i = received.length - 1; i >= from; i--) {
            mails.add(toDto(received[i]));
        }
        return mails;
    }

    private static CapturedMailDto toDto(MimeMessage message) {
        try {
            return new CapturedMailDto(
                    join(message.getFrom()),
                    join(message.getRecipients(Message.RecipientType.TO)),
                    message.getSubject(),
                    extractBody(message),
                    message.getSentDate() != null ? message.getSentDate().toInstant() : Instant.now()
            );
        } catch (MessagingException | IOException exception) {
            return new CapturedMailDto("", "", "(illisible)", exception.getMessage(), Instant.now());
        }
    }

    private static String join(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }
        return String.join(", ", Arrays.stream(addresses).map(Address::toString).toList());
    }

    private static String extractBody(MimeMessage message) throws MessagingException, IOException {
        Object content = message.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof MimeMultipart multipart) {
            String html = findPart(multipart, "text/html");
            if (html != null) {
                return html;
            }
            String plain = findPart(multipart, "text/plain");
            if (plain != null) {
                return "<pre style=\"white-space:pre-wrap;padding:1rem;\">" + esc(plain) + "</pre>";
            }
        }
        return content != null ? content.toString() : "";
    }

    private static String findPart(MimeMultipart multipart, String mimeType)
            throws MessagingException, IOException {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object nested = part.getContent();
            if (nested instanceof MimeMultipart nestedMultipart) {
                String found = findPart(nestedMultipart, mimeType);
                if (found != null) {
                    return found;
                }
            }
            String partType = part.getContentType();
            if (partType != null && partType.toLowerCase().startsWith(mimeType)) {
                Object value = part.getContent();
                return value != null ? value.toString() : null;
            }
        }
        return null;
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
