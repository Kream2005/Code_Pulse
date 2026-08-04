package com.stage.backend.kafka.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@EnableConfigurationProperties(ExternalApiProperties.class)
@Configuration
public class ExternalApiClientConfig {

    @Bean
    RestClient externalApiRestClient(ExternalApiProperties externalApiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(externalApiProperties.connectTimeout());
        requestFactory.setReadTimeout(externalApiProperties.readTimeout());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}