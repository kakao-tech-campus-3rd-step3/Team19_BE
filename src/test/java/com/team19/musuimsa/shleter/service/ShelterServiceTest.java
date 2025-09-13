package com.team19.musuimsa.shleter.service;

import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.dto.NearbyShelterResponse;
import com.team19.musuimsa.shelter.dto.ShelterResponse;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.shelter.service.ShelterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShelterServiceTest {

    @Mock
    ShelterRepository repository;
    @InjectMocks
    ShelterService service;

    @Test
    @DisplayName("findNearbyShelters - 엔티티 리스트를 NearbyShelterResponse로 매핑한다. ")
    void findNearbyShelters_mapsEntitiesToDtos() {
        // given
        double userLat = 37.5665;
        double userLng = 126.9780;

        Shelter s1 = Shelter.builder()
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

        Shelter s2 = Shelter.builder()
                .shelterId(2L)
                .name("을지로 무더위 쉼터")
                .address("서울 중구 을지로 45")
                .latitude(BigDecimal.valueOf(37.5651))
                .longitude(BigDecimal.valueOf(126.9895))
                .weekdayOpenTime(LocalTime.of(9, 0))
                .weekdayCloseTime(LocalTime.of(18, 0))
                .weekendOpenTime(LocalTime.of(10, 0))
                .weekendCloseTime(LocalTime.of(16, 0))
                .capacity(100)
                .isOutdoors(false)
                .fanCount(2)
                .airConditionerCount(1)
                .totalRating(19)
                .reviewCount(5)
                .photoUrl("https://example.com/shelter2.jpg")
                .build();

        when(repository.findNearbyShelters(userLat, userLng, 1000)).thenReturn(List.of(s1, s2));

        // when
        List<NearbyShelterResponse> list = service.findNearbyShelters(userLat, userLng);

        // then
        assertThat(list).hasSize(2);

        NearbyShelterResponse dto1 = list.get(0);
        assertSoftly(softly -> {
            assertThat(dto1.shelterId()).isEqualTo(1L);
            assertThat(dto1.name()).isEqualTo("종로 무더위 쉼터");
            assertThat(dto1.address()).isEqualTo("서울 종로구 세종대로 175");
            assertThat(dto1.latitude()).isEqualTo(37.5665);
            assertThat(dto1.longitude()).isEqualTo(126.9780);
            assertThat(dto1.distance()).isEqualTo("0.0km");
            assertThat(dto1.operatingHoursResponse().weekday()).isEqualTo("09:00~18:00");
            assertThat(dto1.operatingHoursResponse().weekend()).isEqualTo("10:00~16:00");
            assertThat(dto1.isOutdoors()).isEqualTo(true);
            assertThat(dto1.averageRating()).isEqualTo(2.8); // 14/5
            assertThat(dto1.photoUrl()).isEqualTo("https://example.com/shelter1.jpg");
        });

        NearbyShelterResponse dto2 = list.get(1);
        assertSoftly(softly -> {
            assertThat(dto2.shelterId()).isEqualTo(2L);
            assertThat(dto2.name()).isEqualTo("을지로 무더위 쉼터");
            assertThat(dto2.address()).isEqualTo("서울 중구 을지로 45");
            assertThat(dto2.latitude()).isEqualTo(37.5651);
            assertThat(dto2.longitude()).isEqualTo(126.9895);
            assertThat(dto2.distance()).endsWith("km");
            assertThat(dto2.distance()).startsWith("1.");
            assertThat(dto2.operatingHoursResponse().weekday()).isEqualTo("09:00~18:00");
            assertThat(dto2.operatingHoursResponse().weekend()).isEqualTo("10:00~16:00");
            assertThat(dto2.isOutdoors()).isEqualTo(false);
            assertThat(dto2.averageRating()).isEqualTo(3.8); // 19/5
            assertThat(dto2.photoUrl()).isEqualTo("https://example.com/shelter2.jpg");
        });
    }

    @DisplayName("getShelter - 존재하지 않으면 ShelterNotFoundException이 발생한다. ")
    @Test
    void getShelter_throws_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getShelter(99L, 37.0, 127.0))
                .isInstanceOf(ShelterNotFoundException.class);
    }

    @DisplayName("getShelter - 존재하면 ShelterResponse로 매핑된다. ")
    @Test
    void getShelter_returnsDetailDto() {
        // given
        double userLat = 37.5665;
        double userLng = 126.9780;

        Shelter s = Shelter.builder()
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

        when(repository.findById(1L)).thenReturn(Optional.of(s));

        // when
        ShelterResponse dto = service.getShelter(1L, userLat, userLng);

        // then
        assertSoftly(softly -> {
            assertThat(dto.shelterId()).isEqualTo(1L);
            assertThat(dto.name()).isEqualTo("종로 무더위 쉼터");
            assertThat(dto.address()).isEqualTo("서울 종로구 세종대로 175");
            assertThat(dto.latitude()).isEqualTo(37.5665);
            assertThat(dto.longitude()).isEqualTo(126.9780);
            assertThat(dto.distance()).isEqualTo("0.0km");
            assertThat(dto.operatingHoursResponse().weekday()).isEqualTo("09:00~18:00");
            assertThat(dto.operatingHoursResponse().weekend()).isEqualTo("10:00~16:00");
            assertThat(dto.capacity()).isEqualTo(50);
            assertThat(dto.isOutdoors()).isEqualTo(true);
            assertThat(dto.coolingEquipment().fanCount()).isEqualTo(3);
            assertThat(dto.coolingEquipment().airConditionerCount()).isEqualTo(1);
            assertThat(dto.totalRating()).isEqualTo(14);
            assertThat(dto.reviewCount()).isEqualTo(5);
            assertThat(dto.photoUrl()).isEqualTo("https://example.com/shelter1.jpg");
        });
    }
}
