package com.team19.musuimsa.shelter.domain;

import com.team19.musuimsa.shelter.dto.UpdateResultResponse;
import com.team19.musuimsa.shelter.dto.external.ExternalShelterItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

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
                .photoUrl(null)
                .build();
    }

    private ExternalShelterItem createExternalItemWithSameData(String facilityType) {
        return new ExternalShelterItem(
                1L, "기존 쉼터", "기존 주소",
                new BigDecimal("37.12345678"), new BigDecimal("127.12345678"),
                10, 1, 1,
                "0900", "1800", "1000", "1700", facilityType
        );
    }

    @Nested
    @DisplayName("updateShelterInfo 메소드는")
    class UpdateShelterInfoTest {

        @Test
        @DisplayName("변경 사항이 없으면 false를 반환한다")
        void returnsFalse_whenNoChanges() {
            ExternalShelterItem item = createExternalItemWithSameData("001");

            UpdateResultResponse res = shelter.updateShelterInfo(
                    item, baseWeekdayOpen, baseWeekdayClose, baseWeekendOpen, baseWeekendClose
            );

            assertThat(res.isChanged()).isFalse();
            assertThat(res.locationChanged()).isFalse();
        }

        @Test
        @DisplayName("이름만 변경될 경우, 이름만 수정하고 true를 반환한다")
        void returnsTrue_whenOnlyNameChanges() {
            ExternalShelterItem item = new ExternalShelterItem(
                    1L, "새로운 쉼터", "기존 주소",
                    new BigDecimal("37.12345678"), new BigDecimal("127.12345678"),
                    10, 1, 1,
                    "0900", "1800", "1000", "1700", "001"
            );

            UpdateResultResponse res = shelter.updateShelterInfo(
                    item, baseWeekdayOpen, baseWeekdayClose, baseWeekendOpen, baseWeekendClose
            );

            assertSoftly(softly -> {
                softly.assertThat(res.isChanged()).isTrue();
                softly.assertThat(res.locationChanged()).isFalse();
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
        @DisplayName("비지오(이름/주소/시간/수용인원 등)만 변경되면 locationChanged=false")
        void nonGeoChanged_locationFalse() {
            ExternalShelterItem item = new ExternalShelterItem(
                    1L, "을지로 무더위 쉼터 2", "서울 중구 을지로 45-2",
                    new BigDecimal("37.12345678"), new BigDecimal("127.12345678"), // 위경도 동일
                    100, 2, 1,
                    "0900", "1800", "1000", "1600",
                    "002"
            );

            UpdateResultResponse res = shelter.updateShelterInfo(
                    item, LocalTime.of(9, 0), LocalTime.of(18, 0),
                    LocalTime.of(10, 0), LocalTime.of(16, 0)
            );

            assertThat(res.isChanged()).isTrue();
            assertThat(res.locationChanged()).isFalse();
            assertThat(shelter.getName()).isEqualTo("을지로 무더위 쉼터 2");
            assertThat(shelter.getAddress()).isEqualTo("서울 중구 을지로 45-2");
            assertThat(shelter.getCapacity()).isEqualTo(100);
            assertThat(shelter.getIsOutdoors()).isTrue();
        }

        @Test
        @DisplayName("위/경도가 변경되면 locationChanged=true")
        void geoChanged_locationTrue() {
            ExternalShelterItem item = new ExternalShelterItem(
                    1L, "전부 새로운 쉼터", "전부 새로운 주소",
                    new BigDecimal("35.98765432"), new BigDecimal("128.98765432"), // 위경도 변경
                    20, 2, 3,
                    "0800", "2000", "1100", "1600", "002"
            );

            UpdateResultResponse res = shelter.updateShelterInfo(
                    item, LocalTime.of(8, 0), LocalTime.of(20, 0),
                    LocalTime.of(11, 0), LocalTime.of(16, 0)
            );

            assertSoftly(softly -> {
                softly.assertThat(res.isChanged()).isTrue();
                softly.assertThat(res.locationChanged()).isTrue();
                softly.assertThat(shelter.getLatitude()).isEqualByComparingTo("35.98765432");
                softly.assertThat(shelter.getLongitude()).isEqualByComparingTo("128.98765432");
                softly.assertThat(shelter.getIsOutdoors()).isTrue();
            });
        }

        @Test
        @DisplayName("시설 타입 코드가 '002'면 isOutdoors=true")
        void updatesIsOutdoorsToTrue_whenFacilityTypeChangesToOutdoor() {
            ExternalShelterItem item = createExternalItemWithSameData("002");

            UpdateResultResponse res = shelter.updateShelterInfo(
                    item, baseWeekdayOpen, baseWeekdayClose, baseWeekendOpen, baseWeekendClose
            );

            assertThat(res.isChanged()).isTrue();
            assertThat(res.locationChanged()).isFalse();
            assertThat(shelter.getIsOutdoors()).isTrue();
        }

        @Test
        @DisplayName("기존 true였다가 시설 타입이 '002'가 아니면 false로 변경")
        void updatesIsOutdoorsToFalse_whenFacilityTypeChangesToIndoor() {
            // 먼저 야외로 변경해 둠
            ExternalShelterItem outdoorItem = createExternalItemWithSameData("002");
            shelter.updateShelterInfo(outdoorItem, baseWeekdayOpen, baseWeekdayClose,
                    baseWeekendOpen, baseWeekendClose);
            assertThat(shelter.getIsOutdoors()).isTrue();

            // 실내로 변경
            ExternalShelterItem indoorItem = createExternalItemWithSameData("001");
            UpdateResultResponse res = shelter.updateShelterInfo(
                    indoorItem, baseWeekdayOpen, baseWeekdayClose, baseWeekendOpen, baseWeekendClose
            );

            assertThat(res.isChanged()).isTrue();
            assertThat(res.locationChanged()).isFalse();
            assertThat(shelter.getIsOutdoors()).isFalse();
        }
    }

    @Nested
    @DisplayName("updatePhotoUrl 메소드는")
    class UpdatePhotoUrlTest {

        @Test
        @DisplayName("null/blank 값이면 false 반환하고 변경하지 않는다")
        void returnsFalse_whenNullOrBlank() {
            assertThat(shelter.updatePhotoUrl(null)).isFalse();
            assertThat(shelter.updatePhotoUrl("")).isFalse();
            assertThat(shelter.getPhotoUrl()).isNull();
        }

        @Test
        @DisplayName("같은 URL로 호출하면 false 반환")
        void returnsFalse_whenSameUrl() {
            String url = "https://mock.example/shelters/1.jpg";
            assertThat(shelter.updatePhotoUrl(url)).isTrue();   // 최초 설정
            assertThat(shelter.updatePhotoUrl(url)).isFalse();  // 동일 값
            assertThat(shelter.getPhotoUrl()).isEqualTo(url);
        }

        @Test
        @DisplayName("다른 URL이면 값 업데이트 후 true 반환")
        void returnsTrue_whenDifferentUrl() {
            String oldUrl = "https://mock.example/shelters/1.jpg";
            String newUrl = "https://mock.example/shelters/1_v2.jpg";

            shelter.updatePhotoUrl(oldUrl);
            boolean changed = shelter.updatePhotoUrl(newUrl);

            assertThat(changed).isTrue();
            assertThat(shelter.getPhotoUrl()).isEqualTo(newUrl);
        }
    }

    @Nested
    @DisplayName("toShelter 정적 팩토리 메소드는")
    class ToShelterTest {

        @Test
        @DisplayName("ExternalShelterItem을 Shelter 엔티티로 올바르게 변환한다")
        void convertsExternalItemToShelterEntity() {
            ExternalShelterItem item = new ExternalShelterItem(
                    1L, "테스트 쉼터", "테스트 주소",
                    new BigDecimal("37.123"), new BigDecimal("127.456"),
                    50, 5, 2,
                    "0900", "1800", "1000", "1700", "002"
            );

            Shelter converted = Shelter.toShelter(item);

            assertSoftly(softly -> {
                softly.assertThat(converted.getShelterId()).isEqualTo(1L);
                softly.assertThat(converted.getName()).isEqualTo("테스트 쉼터");
                softly.assertThat(converted.getAddress()).isEqualTo("테스트 주소");
                softly.assertThat(converted.getLatitude()).isEqualByComparingTo("37.123");
                softly.assertThat(converted.getLongitude()).isEqualByComparingTo("127.456");
                softly.assertThat(converted.getCapacity()).isEqualTo(50);
                softly.assertThat(converted.getFanCount()).isEqualTo(5);
                softly.assertThat(converted.getAirConditionerCount()).isEqualTo(2);
                softly.assertThat(converted.getWeekdayOpenTime()).isEqualTo(LocalTime.of(9, 0));
                softly.assertThat(converted.getWeekdayCloseTime()).isEqualTo(LocalTime.of(18, 0));
                softly.assertThat(converted.getWeekendOpenTime()).isEqualTo(LocalTime.of(10, 0));
                softly.assertThat(converted.getWeekendCloseTime()).isEqualTo(LocalTime.of(17, 0));
                softly.assertThat(converted.getIsOutdoors()).isTrue();
                softly.assertThat(converted.getPhotoUrl()).isNull();
            });
        }
    }
}
