package com.stage.backend.config.notification;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

@Configuration
@Profile("standalone")
@ConditionalOnProperty(name = "codepulse.notification.embedded-smtp", havingValue = "true")
@Slf4j
public class EmbeddedSmtpConfig {

    private static final int SMTP_PORT = 1025;
    private GreenMail started;

    @Bean
    public GreenMail greenMail() {
        if (!smtpPortFree(SMTP_PORT)) {
            log.info("SMTP already listening on :{} — embedded GreenMail skipped", SMTP_PORT);
            return null;
        }
        ServerSetup setup = new ServerSetup(SMTP_PORT, "127.0.0.1", ServerSetup.PROTOCOL_SMTP);
        setup.setServerStartupTimeout(10_000);
        GreenMail greenMail = new GreenMail(setup);
        greenMail.start();
        started = greenMail;
        log.info("Embedded SMTP (GreenMail) on 127.0.0.1:{} — GET /dev/mailbox", SMTP_PORT);
        return greenMail;
    }

    @PreDestroy
    public void stop() {
        if (started != null) {
            started.stop();
        }
    }

    private static boolean smtpPortFree(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
