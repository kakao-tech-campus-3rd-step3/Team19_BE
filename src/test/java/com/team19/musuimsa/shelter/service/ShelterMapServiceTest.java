package com.team19.musuimsa.shelter.service;

import com.team19.musuimsa.shelter.dto.map.MapBoundsRequest;
import com.team19.musuimsa.shelter.dto.map.MapResponse;
import com.team19.musuimsa.shelter.dto.map.MapShelterResponse;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShelterMapServiceTest {

    @Test
    @DisplayName("zoom<13 또는 스팬>3도 → cluster 레벨 반환")
    void clusterLevel_whenZoomLowOrSpanWide() {
        ShelterRepository repo = mock(ShelterRepository.class);
        ShelterMapService svc = new ShelterMapService(repo);

        // repo summary 결과를 3개로 가정 (클러스터링 결과 count는 그룹 수)
        List<MapShelterResponse> three = List.of(
                new MapShelterResponse(1L, "A", 37.1, 127.1, true, 10, null, null, null),
                new MapShelterResponse(2L, "B", 37.1001, 127.1, false, 20, null, null, null),
                new MapShelterResponse(3L, "C", 37.5, 127.5, true, 30, null, null, null)
        );
        when(repo.findSummaryInBbox(any(), any(), any(), any(), any())).thenReturn(three);

        // zoom 12 → cluster
        MapResponse r1 = svc.getByBbox(new MapBoundsRequest(
                37.0, 127.0, 37.6, 127.6, 12, null, null
        ));
        assertThat(r1.level()).isEqualTo("cluster");
        assertThat(r1.items()).isNotEmpty();
        verify(repo, times(1)).findSummaryInBbox(
                any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any(BigDecimal.class), any());

        // spanLat > 3.0 → cluster
        MapResponse r2 = svc.getByBbox(new MapBoundsRequest(
                30.0, 120.0, 34.5, 122.0, 15, null, null
        ));
        assertThat(r2.level()).isEqualTo("cluster");
    }

    @Test
    @DisplayName("13≤zoom<16 → summary, 16≤zoom → detail")
    void summaryAndDetailLevels_byZoom() {
        ShelterRepository repo = mock(ShelterRepository.class);
        ShelterMapService svc = new ShelterMapService(repo);

        when(repo.findSummaryInBbox(any(), any(), any(), any(), any()))
                .thenReturn(List.of(new MapShelterResponse(1L, "A", 37.1, 127.1, true, 10, null, null, null)));
        when(repo.findDetailInBbox(any(), any(), any(), any(), any()))
                .thenReturn(List.of(new MapShelterResponse(1L, "A", 37.1, 127.1, true, 10, "u", "09:00", "18:00")));
        when(repo.countInBbox(any(), any(), any(), any())).thenReturn(42);

        // zoom 14 → summary
        MapResponse summary = svc.getByBbox(new MapBoundsRequest(
                37.0, 127.0, 37.2, 127.2, 14, 0, 200
        ));
        assertThat(summary.level()).isEqualTo("summary");
        assertThat(summary.total()).isEqualTo(42);
        verify(repo, times(1)).findSummaryInBbox(any(), any(), any(), any(), any());

        // zoom 16 → detail
        MapResponse detail = svc.getByBbox(new MapBoundsRequest(
                37.0, 127.0, 37.02, 127.02, 16, 0, 200
        ));
        assertThat(detail.level()).isEqualTo("detail");
        assertThat(detail.total()).isEqualTo(42);
        verify(repo, times(1)).findDetailInBbox(any(), any(), any(), any(), any());
    }
}
