package com.team19.musuimsa.shleter.util;

import com.team19.musuimsa.shelter.util.ShelterDtoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ShelterDtoUtilsTest {

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

    @DisplayName("average - total 또는 count가 null/0이면 0.0이 반환된다. ")
    @Test
    void average_returnsZero_onInvalidInputs() {
        assertSoftly(softly -> {
            assertThat(ShelterDtoUtils.average(null, 1)).isEqualTo(0.0);
            assertThat(ShelterDtoUtils.average(10, null)).isEqualTo(0.0);
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
