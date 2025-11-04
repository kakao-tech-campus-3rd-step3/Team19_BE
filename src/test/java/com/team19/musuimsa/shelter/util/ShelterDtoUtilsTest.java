package com.team19.musuimsa.shelter.util;

import com.team19.musuimsa.shelter.domain.Shelter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.offset;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShelterDtoUtilsTest {

    @DisplayName("haversineMeters - 같은 좌표면 0에 가깝다.")
    @Test
    void haversine_zeroDistance() {
        double d = ShelterDtoUtils.haversineMeters(37.5665, 126.9780, 37.5665, 126.9780);
        assertThat(d).isCloseTo(0.0, offset(1e-6));
    }

    @DisplayName("haversineMeters - 알려진 두 지점 사이 거리 대략 일치")
    @Test
    void haversine_knownDistance() {
        // 서울 시청(37.5665,126.9780) ↔ 을지로입구 근방(37.5651,126.9895) ≈ 1.0~1.2km
        double d = ShelterDtoUtils.haversineMeters(37.5665, 126.9780, 37.5651, 126.9895);
        assertThat(d).isCloseTo(1100.0, offset(200.0)); // ±200m 허용
    }

    @DisplayName("formatDistance - 999m 이하는 'Xm', 1000m 이상은 'Y.Ykm'")
    @Test
    void formatDistance_thresholds() {
        assertThat(ShelterDtoUtils.formatDistance(0.0)).isEqualTo("0m");
        assertThat(ShelterDtoUtils.formatDistance(123.4)).isEqualTo("123m");
        // 999.6 → 반올림 1000 → 1.0km
        assertThat(ShelterDtoUtils.formatDistance(999.6)).isEqualTo("1.0km");
        assertThat(ShelterDtoUtils.formatDistance(1500.0)).isEqualTo("1.5km");
    }

    @DisplayName("distanceBetween - 동일 좌표면 '0m', 떨어져 있으면 'Y.Ykm' 형식")
    @Test
    void distanceBetween_formatsString() {
        String zero = ShelterDtoUtils.distanceBetween(37.0, 127.0, 37.0, 127.0);
        assertThat(zero).isEqualTo("0m");

        String approx = ShelterDtoUtils.distanceBetween(37.5665, 126.9780, 37.5651, 126.9895);
        assertThat(approx).endsWith("km");
        assertThat(approx).startsWith("1."); // "1.0km" ~ "1.2km"
    }

    @DisplayName("distanceFrom - Shelter의 좌표를 사용해 거리 문자열을 반환한다.")
    @Test
    void distanceFrom_usesShelterCoords() {
        Shelter shelter = mock(Shelter.class);
        when(shelter.getLatitude()).thenReturn(BigDecimal.valueOf(37.5665));
        when(shelter.getLongitude()).thenReturn(BigDecimal.valueOf(126.9780));

        // 사용자 위치 동일 → 0m
        String d0 = ShelterDtoUtils.distanceFrom(37.5665, 126.9780, shelter);
        assertThat(d0).isEqualTo("0m");

        // 사용자 위치 살짝 이동 → km 형식(1.xkm)
        String d1 = ShelterDtoUtils.distanceFrom(37.5651, 126.9895, shelter);
        assertThat(d1).endsWith("km");
        assertThat(d1).startsWith("1.");
    }

    @DisplayName("formatHours - 둘 다 null이면 빈 문자열이 반환된다. ")
    @Test
    void returnsEmpty_whenBothNull() {
        assertThat(ShelterDtoUtils.formatHours(null, null))
                .isEqualTo("");
    }

    @DisplayName("formatHours - open null이면 '~HH:mm' 형태로 반환된다. ")
    @Test
    void returnsOnlyEnd_whenOpenNull() {
        assertThat(ShelterDtoUtils.formatHours(null, LocalTime.of(18, 0)))
                .isEqualTo("~18:00");
    }

    @DisplayName("formatHours - close null이면 'HH:mm~' 형태로 반환된다. ")
    @Test
    void returnsOnlyStart_whenCloseNull() {
        assertThat(ShelterDtoUtils.formatHours(LocalTime.of(9, 0), null))
                .isEqualTo("09:00~");
    }

    @DisplayName("formatHours - 둘 다 있으면 'HH:mm~HH:mm' 형태로 반환된다. ")
    @Test
    void returnsRange_whenBothPresent() {
        assertThat(ShelterDtoUtils.formatHours(LocalTime.of(9, 0), LocalTime.of(18, 0)))
                .isEqualTo("09:00~18:00");
    }

    @DisplayName("average - total 또는 count가 0이면 0.0이 반환된다. ")
    @Test
    void average_returnsZero_onInvalidInputs() {
        assertSoftly(softly -> {
            assertThat(ShelterDtoUtils.average(0, 1)).isEqualTo(0.0);
            assertThat(ShelterDtoUtils.average(10, 0)).isEqualTo(0.0);
        });
    }

    @DisplayName("average - 올바른 값이면 평균값이 반환된다. ")
    @Test
    void average_returnsQuotient() {
        assertThat(ShelterDtoUtils.average(9, 2))
                .isEqualTo(4.5);
    }
}
