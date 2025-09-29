package com.team19.musuimsa.shelter.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Shelter 엔티티 테스트")
class ShelterTest {

    private Shelter shelter;
    private final LocalTime baseWeekdayOpen = LocalTime.of(9, 0);
    private final LocalTime baseWeekdayClose = LocalTime.of(18, 0);
    private final LocalTime baseWeekendOpen = LocalTime.of(10, 0);
    private final LocalTime baseWeekendClose = LocalTime.of(17, 0);

    @BeforeEach
    void setUp() {
        shelter = Shelter.builder()
                .shelterId(1L)
                .name("기존 쉼터")
                .address("기존 주소")
                .latitude(new BigDecimal("37.12345678"))
                .longitude(new BigDecimal("127.12345678"))
                .capacity(10)
                .isOutdoors(false)
                .fanCount(1)
                .airConditionerCount(1)
                .weekdayOpenTime(baseWeekdayOpen)
                .weekdayCloseTime(baseWeekdayClose)
                .weekendOpenTime(baseWeekendOpen)
                .weekendCloseTime(baseWeekendClose)
                .build();
    }

    private ExternalShelterItem createExternalItemWithSameData(String facilityType) {
        return new ExternalShelterItem(1L, "기존 쉼터", "기존 주소",
                new BigDecimal("37.12345678"), new BigDecimal("127.12345678"),
                10, 1, 1,
                "0900", "1800", "1000", "1700", facilityType);
    }

    @Nested
    @DisplayName("updateShelterInfo 메소드는")
    class UpdateShelterInfoTest {

        @Test
        @DisplayName("변경 사항이 없으면 false를 반환한다")
        void returnsFalse_whenNoChanges() {
            ExternalShelterItem item = createExternalItemWithSameData("001");

            boolean isChanged = shelter.updateShelterInfo(item, baseWeekdayOpen, baseWeekdayClose,
                    baseWeekendOpen, baseWeekendClose);

            assertThat(isChanged).isFalse();
        }

        @Test
        @DisplayName("이름만 변경될 경우, 이름만 수정하고 true를 반환한다")
        void returnsTrue_whenOnlyNameChanges() {
            ExternalShelterItem item = new ExternalShelterItem(1L, "새로운 쉼터", "기존 주소",
                    new BigDecimal("37.12345678"), new BigDecimal("127.12345678"),
                    10, 1, 1,
                    "0900", "1800", "1000", "1700", "001");

            boolean isChanged = shelter.updateShelterInfo(item, baseWeekdayOpen, baseWeekdayClose,
                    baseWeekendOpen, baseWeekendClose);

            assertSoftly(softly -> {
                softly.assertThat(isChanged).isTrue();
                softly.assertThat(shelter.getName()).isEqualTo("새로운 쉼터");
                softly.assertThat(shelter.getAddress()).isEqualTo("기존 주소");
                softly.assertThat(shelter.getLatitude()).isEqualByComparingTo("37.12345678");
                softly.assertThat(shelter.getLongitude()).isEqualByComparingTo("127.12345678");
                softly.assertThat(shelter.getCapacity()).isEqualTo(10);
                softly.assertThat(shelter.getFanCount()).isEqualTo(1);
                softly.assertThat(shelter.getAirConditionerCount()).isEqualTo(1);
                softly.assertThat(shelter.getWeekdayOpenTime()).isEqualTo(baseWeekdayOpen);
                softly.assertThat(shelter.getWeekdayCloseTime()).isEqualTo(baseWeekdayClose);
                softly.assertThat(shelter.getWeekendOpenTime()).isEqualTo(baseWeekendOpen);
                softly.assertThat(shelter.getWeekendCloseTime()).isEqualTo(baseWeekendClose);
                softly.assertThat(shelter.getIsOutdoors()).isFalse();
            });
        }

        @Test
        @DisplayName("모든 필드가 변경될 경우, 모든 필드를 수정하고 true를 반환한다")
        void returnsTrue_whenAllFieldsChange() {
            LocalTime newWeekdayOpen = LocalTime.of(8, 0);
            LocalTime newWeekdayClose = LocalTime.of(20, 0);
            LocalTime newWeekendOpen = LocalTime.of(11, 0);
            LocalTime newWeekendClose = LocalTime.of(16, 0);

            ExternalShelterItem item = new ExternalShelterItem(1L, "전부 새로운 쉼터", "전부 새로운 주소",
                    new BigDecimal("35.98765432"), new BigDecimal("128.98765432"),
                    20, 2, 3,
                    "0800", "2000", "1100", "1600", "002");

            boolean isChanged = shelter.updateShelterInfo(item, newWeekdayOpen, newWeekdayClose,
                    newWeekendOpen, newWeekendClose);

            assertSoftly(softly -> {
                softly.assertThat(isChanged).isTrue();
                softly.assertThat(shelter.getName()).isEqualTo("전부 새로운 쉼터");
                softly.assertThat(shelter.getAddress()).isEqualTo("전부 새로운 주소");
                softly.assertThat(shelter.getLatitude()).isEqualByComparingTo("35.98765432");
                softly.assertThat(shelter.getLongitude()).isEqualByComparingTo("128.98765432");
                softly.assertThat(shelter.getCapacity()).isEqualTo(20);
                softly.assertThat(shelter.getFanCount()).isEqualTo(2);
                softly.assertThat(shelter.getAirConditionerCount()).isEqualTo(3);
                softly.assertThat(shelter.getWeekdayOpenTime()).isEqualTo(newWeekdayOpen);
                softly.assertThat(shelter.getWeekdayCloseTime()).isEqualTo(newWeekdayClose);
                softly.assertThat(shelter.getWeekendOpenTime()).isEqualTo(newWeekendOpen);
                softly.assertThat(shelter.getWeekendCloseTime()).isEqualTo(newWeekendClose);
                softly.assertThat(shelter.getIsOutdoors()).isTrue();
            });
        }

        @Test
        @DisplayName("시설 타입 코드가 '002'로 변경되면 isOutdoors 필드를 true로 업데이트한다")
        void updatesIsOutdoorsToTrue_whenFacilityTypeChangesToOutdoor() {
            ExternalShelterItem item = createExternalItemWithSameData("002");

            boolean isChanged = shelter.updateShelterInfo(item, baseWeekdayOpen, baseWeekdayClose,
                    baseWeekendOpen, baseWeekendClose);

            assertThat(isChanged).isTrue();
            assertThat(shelter.getIsOutdoors()).isTrue();
        }

        @Test
        @DisplayName("isOutdoors가 true였다가 시설 타입 코드가 '002'가 아니게 되면 false로 업데이트한다")
        void updatesIsOutdoorsToFalse_whenFacilityTypeChangesToIndoor() {
            ExternalShelterItem outdoorItem = createExternalItemWithSameData("002");
            shelter.updateShelterInfo(outdoorItem, baseWeekdayOpen, baseWeekdayClose,
                    baseWeekendOpen, baseWeekendClose);
            assertThat(shelter.getIsOutdoors()).isTrue();

            ExternalShelterItem indoorItem = createExternalItemWithSameData("001");
            boolean isChanged = shelter.updateShelterInfo(indoorItem, baseWeekdayOpen,
                    baseWeekdayClose, baseWeekendOpen, baseWeekendClose);

            assertThat(isChanged).isTrue();
            assertThat(shelter.getIsOutdoors()).isFalse();
        }
    }

    @Nested
    @DisplayName("toShelter 정적 팩토리 메소드는")
    class ToShelterTest {

        @Test
        @DisplayName("ExternalShelterItem을 Shelter 엔티티로 올바르게 변환한다")
        void convertsExternalItemToShelterEntity() {
            ExternalShelterItem item = new ExternalShelterItem(1L, "테스트 쉼터", "테스트 주소",
                    new BigDecimal("37.123"), new BigDecimal("127.456"),
                    50, 5, 2,
                    "0900", "1800", "1000", "1700", "002");

            Shelter convertedShelter = Shelter.toShelter(item);

            assertSoftly(softly -> {
                softly.assertThat(convertedShelter.getShelterId()).isEqualTo(1L);
                softly.assertThat(convertedShelter.getName()).isEqualTo("테스트 쉼터");
                softly.assertThat(convertedShelter.getAddress()).isEqualTo("테스트 주소");
                softly.assertThat(convertedShelter.getLatitude()).isEqualByComparingTo("37.123");
                softly.assertThat(convertedShelter.getLongitude()).isEqualByComparingTo("127.456");
                softly.assertThat(convertedShelter.getCapacity()).isEqualTo(50);
                softly.assertThat(convertedShelter.getFanCount()).isEqualTo(5);
                softly.assertThat(convertedShelter.getAirConditionerCount()).isEqualTo(2);
                softly.assertThat(convertedShelter.getWeekdayOpenTime())
                        .isEqualTo(LocalTime.of(9, 0));
                softly.assertThat(convertedShelter.getWeekdayCloseTime())
                        .isEqualTo(LocalTime.of(18, 0));
                softly.assertThat(convertedShelter.getWeekendOpenTime())
                        .isEqualTo(LocalTime.of(10, 0));
                softly.assertThat(convertedShelter.getWeekendCloseTime())
                        .isEqualTo(LocalTime.of(17, 0));
                softly.assertThat(convertedShelter.getIsOutdoors()).isTrue(); // 002는 true
                softly.assertThat(convertedShelter.getPhotoUrl()).isNull();
            });
        }
    }
}

