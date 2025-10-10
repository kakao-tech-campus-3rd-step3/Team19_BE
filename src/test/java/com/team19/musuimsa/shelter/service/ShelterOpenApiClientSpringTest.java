package com.team19.musuimsa.shelter.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class ShelterOpenApiClientSpringTest {

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = "musuimsa.shelter.api.mode=mock")
    class WhenModeIsMock {

        @Autowired
        ShelterOpenApiClient client;

        @Test
        @DisplayName("musuimsa.shelter.api.mode=mock 이면 MockShelterOpenApiClient 가 주입된다. ")
        void injectsMockClient() {
            assertThat(client).isInstanceOf(MockShelterOpenApiClient.class);
        }
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(properties = "musuimsa.shelter.api.mode=real")
    class WhenModeIsReal {

        @Autowired
        ShelterOpenApiClient client;

        @Test
        @DisplayName("musuimsa.shelter.api.mode=real 이면 Real(=ShelterOpenApiRestClient) 이 주입된다. ")
        void injectsRealClient() {
            assertThat(client).isInstanceOf(ShelterOpenApiRestClient.class);
        }
    }
}
