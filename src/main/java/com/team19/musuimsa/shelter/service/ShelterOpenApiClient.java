package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.exception.external.ExternalApiException;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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

    private final RestClient restClient;

    public ExternalResponse fetchPage(int pageNo) {
        URI uri = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path(path)
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", pageSize)
                .queryParam("returnType", format)
                .build(true)
                .toUri();

        return restClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new ExternalApiException(req.getURI().toString());
                })
                .body(ExternalResponse.class);
    }
}
