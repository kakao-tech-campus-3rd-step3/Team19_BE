package com.team19.musuimsa.shelter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "musuimsa.shelter.api.mode",
        havingValue = "mock"
)
public class MockShelterOpenApiClient implements ShelterOpenApiClient {

    private final ObjectMapper objectMapper;

    @Override
    public ExternalResponse fetchPage(int pageNo) {
        String path = "fixtures/shelter/page-" + pageNo + ".json";
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new FileNotFoundException(path);
            }
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, ExternalResponse.class);
        } catch (IOException e) {
            log.warn("Fixture 읽기 실패. 더미로 폴백합니다. path={}, cause={}", path, e.toString());
            return dummy(pageNo);
        }
    }

    private ExternalResponse dummy(int pageNo) {
        ExternalShelterItem item = new ExternalShelterItem(
                99999999L, // RSTR_FCLTY_NO
                "쉼터 명칭", // RSTR_NM
                "쉼터 도로명 상세 주소", // RN_DTL_ADRES
                new BigDecimal("37.3110768"), // LA
                new BigDecimal("126.8296609"), // LO
                30, // USE_PSBL_NMPR
                null, // COLR_HOLD_ELEFN
                null, // COLR_HOLD_ARCNDTN
                "0900", "1800", // 평일 운영시각
                "1000", "2000", // 주말 운영시각
                "002" // FCLTY_TY (야외)
        );

        return new ExternalResponse(
                new ExternalResponse.Header("NORMAL SERVICE", "00", null),
                30, // numOfRows
                pageNo,
                59638, // totalCount
                List.of(item)
        );
    }
}


