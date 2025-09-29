package com.team19.musuimsa.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.service.ShelterOpenApiClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 1. spring.batch.job.enabled=false 프로퍼티 추가
@SpringBootTest(properties = {"spring.batch.job.enabled=false"})
@SpringBatchTest
class ShelterImportJobIntegrationTest {

    // 테스트 시에만 사용될 가짜(Fake) API 클라이언트 구현
    public static class FakeShelterOpenApiClient implements ShelterOpenApiClient {

        private final Map<Integer, ExternalResponse> responses = new ConcurrentHashMap<>();

        @Override
        public ExternalResponse fetchPage(int pageNo) {
            // 설정된 응답이 없으면 빈 데이터를 담은 응답을 반환
            return responses.getOrDefault(pageNo,
                    new ExternalResponse(null, 0, pageNo, 0, List.of()));
        }

        public void addResponse(int pageNo, ExternalResponse response) {
            this.responses.put(pageNo, response);
        }

        public void clear() {
            this.responses.clear();
        }
    }

    // 테스트 실행 시, 실제 Bean 대신 여기서 등록한 가짜 Bean을 사용하도록 설정
    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public ShelterOpenApiClient shelterOpenApiClient() {
            return new FakeShelterOpenApiClient();
        }
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private ShelterOpenApiClient shelterOpenApiClient; // 주입되는 것은 FakeShelterOpenApiClient

    @BeforeEach
    void setUp() {
        Shelter shelter1 = Shelter.builder().shelterId(1L).name("수정될 쉼터 (이름)")
                .address("수정될 쉼터 (주소)").latitude(new BigDecimal("37.00000000"))
                .longitude(new BigDecimal("127.00000000")).capacity(10).build();
        Shelter shelter2 = Shelter.builder().shelterId(2L).name("변경 없는 쉼터").address("변경 없는 쉼터 주소")
                .latitude(new BigDecimal("35.00000000")).longitude(new BigDecimal("128.00000000"))
                .capacity(20).build();
        shelterRepository.saveAll(List.of(shelter1, shelter2));
    }

    @AfterEach
    void tearDown() {
        shelterRepository.deleteAllInBatch();
        // 각 테스트 후 가짜 클라이언트의 상태를 초기화
        ((FakeShelterOpenApiClient) shelterOpenApiClient).clear();
    }

    @DisplayName("shelterImportJob 실행 시, 외부 데이터와 다른 쉼터 정보만 업데이트된다.")
    @Test
    void shelterUpdateJob_updatesOnlyChangedData() throws Exception {
        // given: 가짜 클라이언트에 Mock 데이터 설정
        FakeShelterOpenApiClient fakeClient = (FakeShelterOpenApiClient) shelterOpenApiClient;

        ExternalShelterItem apiItem1_updated = new ExternalShelterItem(1L, "수정된 쉼터 (이름)",
                "수정된 쉼터 (주소)", new BigDecimal("37.12345678"), new BigDecimal("127.12345678"), 15,
                null, null, null, null, null, null, null);
        ExternalShelterItem apiItem2_same = new ExternalShelterItem(2L, "변경 없는 쉼터", "변경 없는 쉼터 주소",
                new BigDecimal("35.00000000"), new BigDecimal("128.00000000"), 20, null, null, null,
                null, null, null, null);
        ExternalResponse page1Response = new ExternalResponse(null, 2, 1, 2,
                List.of(apiItem1_updated, apiItem2_same));

        fakeClient.addResponse(1, page1Response);

        // when: 배치 작업 실행
        JobParameters jobParameters = jobLauncherTestUtils.getUniqueJobParameters();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // then: 배치 작업 결과 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // then: DB 데이터 검증
        Shelter updatedShelter = shelterRepository.findById(1L).orElseThrow();
        // 2. isEqualByComparingTo로 비교 방식 변경
        assertThat(updatedShelter.getLatitude()).isEqualByComparingTo("37.12345678");
        assertThat(updatedShelter.getLongitude()).isEqualByComparingTo("127.12345678");
        assertThat(updatedShelter.getCapacity()).isEqualTo(15);

        Shelter sameShelter = shelterRepository.findById(2L).orElseThrow();
        assertThat(sameShelter.getName()).isEqualTo("변경 없는 쉼터");
        assertThat(sameShelter.getCapacity()).isEqualTo(20);
    }
}