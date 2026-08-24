package com.stage.backend.controlleur.notification;

import com.icegreen.greenmail.util.GreenMail;
import com.stage.backend.dto.notification.CapturedMailDto;
import com.stage.backend.email.NotificationEmailSender;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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
                <html><head><meta charset="utf-8"><title>CodePulse mailbox</title>
                <style>
                  body { font-family: sans-serif; max-width: 880px; margin: 2rem auto; color: #1e293b; }
                  a { color: #2563eb; }
                  article { border: 1px solid #e2e8f0; border-radius: 8px; padding: 1rem 1.2rem; margin: 1rem 0; }
                  pre { white-space: pre-wrap; background: #f8fafc; padding: 0.8rem; }
                </style></head><body>
                <h1>Demo mailbox</h1>
                <p>Local GreenMail (not real internet mail).
                <a href="/dev/mailbox/send-test">Send a test email</a> ·
                <a href="/dev/mailbox">Refresh</a></p>
                """);
        if (mails.isEmpty()) {
            html.append("<p>No messages yet. Use “Send a test email”, or wait for a notification/relance.</p>");
        }
        for (CapturedMailDto mail : mails) {
            html.append("<article><p><b>")
                    .append(esc(mail.subject()))
                    .append("</b><br>To: ")
                    .append(esc(mail.to()))
                    .append("<br>")
                    .append(esc(String.valueOf(mail.sentAt())))
                    .append("</p><pre>")
                    .append(esc(mail.body()))
                    .append("</pre></article>");
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
            return new CapturedMailDto("", "", "(unreadable)", exception.getMessage(), Instant.now());
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
        return content != null ? content.toString() : "";
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
