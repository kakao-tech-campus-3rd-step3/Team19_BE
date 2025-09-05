package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShelterOpenApiClient {

    @Value("${musuimsa.shelter.api.base-url}")
    private String baseUrl;

    @Value("${musuimsa.shelter.api.path}")
    private String path;

    @Value("${musuimsa.shelter.api.service-key}")
    private String serviceKey;

    @Value("${musuimsa.shelter.api.page-size}")
    private int pageSize;

    @Value("${musuimsa.shelter.api.format}")
    private String format;

    private final @Qualifier("shelterWebClient")
    WebClient webClient;

    public ExternalResponse fetchPage(int pageNo) {
        String url = baseUrl + path;
        try {
            return webClient.get()
                    .uri(uri -> uri
                            .path(url.replaceFirst("^https?://[^/]+", ""))
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", pageSize)
                            .queryParam("returnType", format)
                            .build())
                    .retrieve()
                    .bodyToMono(ExternalResponse.class)
                    .block();
        } catch (Exception e) {
            String full = String.format("%s%s?pageNo=%d&numOfRows=%d&returnType=%s",
                    baseUrl, path, pageNo, pageSize, format);
            throw new ExternalApiException(full, e);
        }
    }

    public int pageSize() {
        return pageSize;
    }

    @PostConstruct
    void logConf() {
        log.info("[ShelterAPI] keyLen={}, base={}, path={}, pageSize={}",
                (serviceKey == null ? 0 : serviceKey.length()), baseUrl, path, pageSize);
    }
}
