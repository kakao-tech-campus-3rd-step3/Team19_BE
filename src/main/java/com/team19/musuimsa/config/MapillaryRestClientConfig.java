package com.team19.musuimsa.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class MapillaryRestClientConfig {

    @Bean(name = "mapillaryRestClient")
    public RestClient mapillaryRestClient(
            RestClient.Builder builder,
            @Value("${mapillary.api-base}") String apiBase,
            @Value("${mapillary.access-token}") String accessToken,
            @Value("${mapillary.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${mapillary.read-timeout-ms}") int readTimeoutMs
    ) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);

        return builder
                .requestFactory(rf)
                .baseUrl(apiBase)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "OAuth " + accessToken)
                .build();
    }
}
