package com.team19.musuimsa.batch;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.external.ExternalResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.service.ShelterOpenApiClient;
import com.team19.musuimsa.shelter.service.ShelterPhotoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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

        @Bean
        @Primary
        public ShelterPhotoService shelterPhotoService() {
            return Mockito.mock(ShelterPhotoService.class);
        }
    }

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private ShelterOpenApiClient shelterOpenApiClient; // 주입되는 것은 FakeShelterOpenApiClient

    @Autowired
    private ShelterPhotoService shelterPhotoService;

    @BeforeEach
    void setUp() {
        Shelter shelter1 = Shelter.builder().shelterId(1L).name("수정될 쉼터 (이름)")
                .address("수정될 쉼터 (주소)").latitude(new BigDecimal("37.00000000"))
                .longitude(new BigDecimal("127.00000000")).capacity(10).isOutdoors(false).build();
        Shelter shelter2 = Shelter.builder().shelterId(2L).name("변경 없는 쉼터").address("변경 없는 쉼터 주소")
                .latitude(new BigDecimal("35.00000000")).longitude(new BigDecimal("128.00000000"))
                .capacity(20).isOutdoors(false).build();
        shelterRepository.saveAll(List.of(shelter1, shelter2));
        when(shelterPhotoService.updatePhoto(Mockito.anyLong())).thenReturn(true);
        clearInvocations(shelterPhotoService);
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

    @DisplayName("배치 실행 시 변경된 쉼터 id에 대해서만 사진 업데이트가 호출된다")
    @Test
    void photoUpdate_calledOnlyForChangedShelters() throws Exception {
        // given: 1번만 값이 변경된 외부 응답
        FakeShelterOpenApiClient fake = (FakeShelterOpenApiClient) shelterOpenApiClient;

        ExternalShelterItem item1Updated = new ExternalShelterItem(
                1L, "수정된 쉼터 (이름)", "수정된 쉼터 (주소)",
                new BigDecimal("37.12345678"), new BigDecimal("127.12345678"),
                15, null, null, null, null, null, null, null);

        ExternalShelterItem item2Same = new ExternalShelterItem(
                2L, "변경 없는 쉼터", "변경 없는 쉼터 주소",
                new BigDecimal("35.00000000"), new BigDecimal("128.00000000"),
                20, null, null, null, null, null, null, null);

        fake.addResponse(1, new ExternalResponse(null, 2, 1, 2, List.of(item1Updated, item2Same)));

        // when
        JobParameters params = jobLauncherTestUtils.getUniqueJobParameters();
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 1번만 호출, 2번은 호출되지 않음
        verify(shelterPhotoService, times(1)).updatePhoto(1L);
        verify(shelterPhotoService, never()).updatePhoto(2L);

        // DB 데이터도 실제로 변경되었는지 확인(기존 테스트와 동일)
        Shelter s1 = shelterRepository.findById(1L).orElseThrow();
        assertThat(s1.getLatitude()).isEqualByComparingTo("37.12345678");
        assertThat(s1.getLongitude()).isEqualByComparingTo("127.12345678");
        assertThat(s1.getCapacity()).isEqualTo(15);

        Shelter s2 = shelterRepository.findById(2L).orElseThrow();
        assertThat(s2.getName()).isEqualTo("변경 없는 쉼터");
        assertThat(s2.getCapacity()).isEqualTo(20);
    }

    @DisplayName("변경사항이 없으면 사진 업데이트가 호출되지 않는다")
    @Test
    void photoUpdate_skippedWhenNoChanges() throws Exception {
        // given: DB와 동일한 외부 응답(두 건 모두 변화 없음)
        FakeShelterOpenApiClient fake = (FakeShelterOpenApiClient) shelterOpenApiClient;

        ExternalShelterItem item1Same = new ExternalShelterItem(
                1L, "수정될 쉼터 (이름)", "수정될 쉼터 (주소)",
                new BigDecimal("37.00000000"), new BigDecimal("127.00000000"),
                10, null, null, null, null, null, null, null);

        ExternalShelterItem item2Same = new ExternalShelterItem(
                2L, "변경 없는 쉼터", "변경 없는 쉼터 주소",
                new BigDecimal("35.00000000"), new BigDecimal("128.00000000"),
                20, null, null, null, null, null, null, null);

        fake.addResponse(1, new ExternalResponse(null, 2, 1, 2, List.of(item1Same, item2Same)));

        // when
        JobParameters params = jobLauncherTestUtils.getUniqueJobParameters();
        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(shelterPhotoService, never()).updatePhoto(Mockito.anyLong());
    }
}