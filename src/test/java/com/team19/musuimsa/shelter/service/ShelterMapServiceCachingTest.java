package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.shelter.dto.OperatingHoursResponse;
import com.team19.musuimsa.shelter.dto.map.MapBoundsRequest;
import com.team19.musuimsa.shelter.dto.map.MapResponse;
import com.team19.musuimsa.shelter.dto.map.MapShelterResponse;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShelterMapServiceCachingTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    @DisplayName("같은 bbox/zoom/page/size → 두 번째 호출은 캐시 적중으로 repo 1번만 호출")
    void cacheHit_onSameKey() {
        runner.run(ctx -> {
            ShelterMapService svc = ctx.getBean(ShelterMapService.class);
            ShelterRepository repo = ctx.getBean(ShelterRepository.class);

            when(repo.findInBbox(any(), any(), any(), any(), any()))
                    .thenReturn(List.of(
                            new MapShelterResponse(1L, "A", "주소 A", 37.1, 127.1, null, true, 10, null, new OperatingHoursResponse(null, null), 0.0)
                    ));
            when(repo.countInBbox(any(), any(), any(), any())).thenReturn(1);

            MapBoundsRequest req = new MapBoundsRequest(37.0, 127.0, 37.2, 127.2, 14, null, null, 0, 200);

            // 1st: DB hit
            MapResponse r1 = svc.getByBbox(req);

            // 호출 기록만 초기화(스텁은 유지)
            clearInvocations(repo);

            // 2nd: cache hit
            MapResponse r2 = svc.getByBbox(req);

            assertThat(r2).isEqualTo(r1);
            verifyNoInteractions(repo);
        });
    }

    @Test
    @DisplayName("page/size/zoom 달라지면 다른 키 → 캐시 미적용")
    void differentKey_onDifferentPageOrZoom() {
        runner.run(ctx -> {
            ShelterMapService svc = ctx.getBean(ShelterMapService.class);
            ShelterRepository repo = ctx.getBean(ShelterRepository.class);

            when(repo.findInBboxWithHours(any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            MapBoundsRequest page0 = new MapBoundsRequest(37.0, 127.0, 37.2, 127.2, 14, null, null, 0, 200);
            MapBoundsRequest page1 = new MapBoundsRequest(37.0, 127.0, 37.2, 127.2, 14, null, null, 1, 200);

            svc.getByBbox(page0); // miss
            svc.getByBbox(page1); // miss (다른 키)

            verify(repo, times(2)).findInBboxWithHours(any(), any(), any(), any(), any());
        });
    }

    @EnableCaching
    @Configuration
    static class TestConfig {
        @Bean
        ShelterMapService shelterMapService(ShelterRepository repo) {
            return new ShelterMapService(repo);
        }

        @Bean
        ShelterRepository shelterRepository() {
            return mock(ShelterRepository.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager("sheltersMap");
        }
    }
}
