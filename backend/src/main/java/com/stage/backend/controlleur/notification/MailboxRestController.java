package com.stage.backend.controlleur.notification;

import com.icegreen.greenmail.util.GreenMail;
import com.stage.backend.dto.notification.CapturedMailDto;
import com.stage.backend.security.SecurityRoles;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping
    @PreAuthorize(SecurityRoles.ADMIN_CODEPULSE)
    public ResponseEntity<List<CapturedMailDto>> lister() {
        GreenMail mailbox = greenMail.getIfAvailable();
        if (mailbox == null) {
            return ResponseEntity.ok(List.of());
        }
        MimeMessage[] received = mailbox.getReceivedMessages();
        int from = Math.max(0, received.length - MAX_MESSAGES);
        List<CapturedMailDto> mails = new ArrayList<>();
        for (int i = received.length - 1; i >= from; i--) {
            mails.add(toDto(received[i]));
        }
        return ResponseEntity.ok(mails);
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
}
