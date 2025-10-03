package com.team19.musuimsa.shelter.domain;

import com.team19.musuimsa.shelter.dto.UpdateResultResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ShelterTest {

    @Test
    @DisplayName("photoUrl이 새 값으로 변경되면 true 반환하고 필드가 갱신된다")
    void updatePhotoUrl_success_whenDifferent() {
        // given
        Shelter shelter = newShelter();

        // when
        boolean changed = shelter.updatePhotoUrl("https://example.com/after.jpg");

        // then
        assertThat(changed).isTrue();
        assertThat(shelter.getPhotoUrl()).isEqualTo("https://example.com/after.jpg");
    }

    @Test
    @DisplayName("photoUrl이 동일하면 false 반환하고 값이 유지된다")
    void updatePhotoUrl_noop_whenSame() {
        // given
        Shelter shelter = newShelter();

        // when
        boolean changed = shelter.updatePhotoUrl("https://example.com/shelter1.jpg");

        // then
        assertThat(changed).isFalse();
        assertThat(shelter.getPhotoUrl()).isEqualTo("https://example.com/shelter1.jpg");
    }

    @Test
    @DisplayName("photoUrl이 null이면 false 반환하고 변경하지 않는다")
    void updatePhotoUrl_noop_whenNull() {
        // given
        Shelter shelter = newShelter();

        // when
        boolean changed = shelter.updatePhotoUrl(null);

        // then
        assertThat(changed).isFalse();
        assertThat(shelter.getPhotoUrl()).isEqualTo("https://example.com/shelter1.jpg");
    }

    @Test
    @DisplayName("photoUrl이 공백이면 false 반환하고 변경하지 않는다")
    void updatePhotoUrl_noop_whenBlank() {
        // given
        Shelter shelter = newShelter();

        // when
        boolean changed = shelter.updatePhotoUrl("   ");

        // then
        assertThat(changed).isFalse();
        assertThat(shelter.getPhotoUrl()).isEqualTo("https://example.com/shelter1.jpg");
    }

    @Test
    @DisplayName("비지오(이름/주소/시간/수용인원 등)만 변경되면 locationChanged=false")
    void updateShelterInfo_nonGeoChanged() {
        // given
        Shelter s = newShelter();
        ExternalShelterItem ext = newItem(
                2L, "을지로 무더위 쉼터 2", "서울 중구 을지로 45-2",
                "37.5665", "126.9780",
                100, 2, 1,
                "0900", "1800", "1000", "1600",
                "002"
        );

        // when
        UpdateResultResponse res = s.updateShelterInfo(
                ext,
                LocalTime.of(9, 00),
                LocalTime.of(18, 00),
                LocalTime.of(10, 0),
                LocalTime.of(16, 0)
        );

        // then
        assertThat(res.isChanged()).isTrue();
        assertThat(res.locationChanged()).isFalse();
        assertThat(s.getName()).isEqualTo("을지로 무더위 쉼터 2");
        assertThat(s.getAddress()).isEqualTo("서울 중구 을지로 45-2");
        assertThat(s.getCapacity()).isEqualTo(100);
        assertThat(s.getFanCount()).isEqualTo(2);
        assertThat(s.getAirConditionerCount()).isEqualTo(1);
        assertThat(s.getWeekdayOpenTime()).isEqualTo(LocalTime.of(9, 00));
        assertThat(s.getWeekdayCloseTime()).isEqualTo(LocalTime.of(18, 00));
        assertThat(s.getWeekendOpenTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(s.getWeekendCloseTime()).isEqualTo(LocalTime.of(16, 0));
        assertThat(s.getIsOutdoors()).isTrue();
    }


    private Shelter newShelter() {
        return Shelter.builder()
                .shelterId(1L)
                .name("종로 무더위 쉼터")
                .address("서울 종로구 세종대로 175")
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .weekdayOpenTime(LocalTime.of(9, 0))
                .weekdayCloseTime(LocalTime.of(18, 0))
                .weekendOpenTime(LocalTime.of(10, 0))
                .weekendCloseTime(LocalTime.of(16, 0))
                .capacity(50)
                .isOutdoors(true)
                .fanCount(3)
                .airConditionerCount(1)
                .totalRating(14)
                .reviewCount(5)
                .photoUrl("https://example.com/shelter1.jpg")
                .build();
    }

    private ExternalShelterItem newItem(
            Long id, String name, String addr,
            String la, String lo, Integer cap,
            Integer fan, Integer ac,
            String wkOpen, String wkClose,
            String weOpen, String weClose,
            String fcltyTy
    ) {
        return new ExternalShelterItem(
                id, name, addr,
                new BigDecimal(la), new BigDecimal(lo),
                cap, fan, ac,
                wkOpen, wkClose, weOpen, weClose,
                fcltyTy
        );
    }
}
