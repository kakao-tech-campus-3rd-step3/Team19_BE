package com.team19.musuimsa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("shelterWebClient")
    public WebClient shelterWebClient(WebClient.Builder builder) {
        return builder.build();
    }
}
