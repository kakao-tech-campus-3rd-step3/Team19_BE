package com.team19.musuimsa.shelter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MockShelterOpenApiClientUnitTest {

    private MockShelterOpenApiClient mockShelterOpenApiClient;

    @BeforeEach
    void setUp() {
        mockShelterOpenApiClient = new MockShelterOpenApiClient(new ObjectMapper());
    }

    @Test
    @DisplayName("클래스패스에 page-1.json, page-2.json 픽스처가 존재한다. ")
    void fixturesExistOnClasspath() {
        URL p1 = getClass().getClassLoader().getResource("fixtures/shelter/page-1.json");
        URL p2 = getClass().getClassLoader().getResource("fixtures/shelter/page-2.json");

        assertSoftly(softly -> {
            softly.assertThat(p1).as("page-1.json 존재").isNotNull();
            softly.assertThat(p2).as("page-2.json 존재").isNotNull();
        });
    }

    @Test
    @DisplayName("pageNo=1 응답을 정상 파싱하고 첫 항목 주요 값이 일치한다. ")
    void page1ParsesAndMatchesExpectedValues() {
        ExternalResponse res = mockShelterOpenApiClient.fetchPage(1);

        assertSoftly(softly -> {
            softly.assertThat(res).isNotNull();
            softly.assertThat(res.header().resultMsg()).isEqualTo("NORMAL SERVICE");
            softly.assertThat(res.header().resultCode()).isEqualTo("00");
            softly.assertThat(res.pageNo()).isEqualTo(1);
            softly.assertThat(res.numOfRows()).isEqualTo(30);
            softly.assertThat(res.body()).hasSize(30);

            ExternalShelterItem first = res.body().get(0);
            softly.assertThat(first.rstrFcltyNo()).isNotNull();
            softly.assertThat(first.rstrNm()).isEqualTo("명학경로당");
            softly.assertThat(first.rnDtlAdres()).contains("대전광역시 서구");
            softly.assertThat(first.la()).isEqualByComparingTo("36.3477");
            softly.assertThat(first.lo()).isEqualByComparingTo("127.3742");
            softly.assertThat(first.wkdayOperBeginTime()).isEqualTo("1300");
            softly.assertThat(first.wkdayOperEndTime()).isEqualTo("1700");
            softly.assertThat(first.wkendHdayOperBeginTime()).isNull();
            softly.assertThat(first.wkendHdayOperEndTime()).isNull();
            softly.assertThat(first.fcltyTy()).isEqualTo("003");
        });
    }

    @Test
    @DisplayName("pageNo=2 응답을 정상 파싱하고 첫 항목 주요 값이 일치한다. ")
    void page2ParsesAndMatchesExpectedValues() {
        ExternalResponse res = mockShelterOpenApiClient.fetchPage(2);

        assertSoftly(softly -> {
            softly.assertThat(res).isNotNull();
            softly.assertThat(res.header().resultMsg()).isEqualTo("NORMAL SERVICE");
            softly.assertThat(res.header().resultCode()).isEqualTo("00");
            softly.assertThat(res.pageNo()).isEqualTo(2);
            softly.assertThat(res.numOfRows()).isEqualTo(30);
            softly.assertThat(res.body()).hasSize(30);

            ExternalShelterItem first = res.body().get(0);
            softly.assertThat(first.rstrNm()).isEqualTo("봉성경로당");
            softly.assertThat(first.rnDtlAdres()).contains("경상남도 남해군");
            softly.assertThat(first.la()).isEqualByComparingTo("34.8120614");
            softly.assertThat(first.lo()).isEqualByComparingTo("127.8837883");
            softly.assertThat(first.wkdayOperBeginTime()).isEqualTo("0900");
            softly.assertThat(first.wkdayOperEndTime()).isEqualTo("1800");
            softly.assertThat(first.wkendHdayOperBeginTime()).isEqualTo("0900");
            softly.assertThat(first.wkendHdayOperEndTime()).isEqualTo("1800");
            softly.assertThat(first.fcltyTy()).isEqualTo("003");
        });
    }

    @Test
    @DisplayName("없는 페이지는 더미 데이터(고정 값)로 폴백한다. ")
    void missingPageFallsBackToDummyWithFixedValues() {
        ExternalResponse res = mockShelterOpenApiClient.fetchPage(9999);

        assertSoftly(softly -> {
            softly.assertThat(res).isNotNull();
            softly.assertThat(res.header().resultCode()).isEqualTo("00");
            softly.assertThat(res.pageNo()).isEqualTo(9999);
            softly.assertThat(res.body()).hasSize(1);

            ExternalShelterItem item = res.body().get(0);
            softly.assertThat(item.rstrFcltyNo()).isEqualTo(99999999L);
            softly.assertThat(item.rstrNm()).isEqualTo("쉼터 명칭");
            softly.assertThat(item.rnDtlAdres()).isEqualTo("쉼터 도로명 상세 주소");
            softly.assertThat(item.la()).isEqualByComparingTo("37.3110768");
            softly.assertThat(item.lo()).isEqualByComparingTo("126.8296609");
            softly.assertThat(item.usePsblNmpr()).isEqualTo(30);
            softly.assertThat(item.wkdayOperBeginTime()).isEqualTo("0900");
            softly.assertThat(item.wkdayOperEndTime()).isEqualTo("1800");
            softly.assertThat(item.wkendHdayOperBeginTime()).isEqualTo("1000");
            softly.assertThat(item.wkendHdayOperEndTime()).isEqualTo("2000");
            softly.assertThat(item.fcltyTy()).isEqualTo("002");
        });
    }
}
