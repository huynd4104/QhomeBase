package com.QhomeBase.iamservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient baseClient(@Value("${base_url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ")
                .build();
    }

    @Bean
    public WebClient baseServiceWebClient(@Value("${base.service.url:http://localhost:8081}") String baseServiceUrl) {
        return WebClient.builder()
                .baseUrl(baseServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

}
