package com.stage.backend.config.notification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(NotificationProperties.class)
@Configuration
public class NotificationConfig {
}
