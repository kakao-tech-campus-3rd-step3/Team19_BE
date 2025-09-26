package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.shelter.dto.external.ExternalResponse;

public interface ShelterOpenApiClient {

    ExternalResponse fetchPage(int pageNo);
}
