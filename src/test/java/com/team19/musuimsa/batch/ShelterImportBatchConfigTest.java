package com.team19.musuimsa.batch;

import static com.team19.musuimsa.batch.ShelterImportBatchConfig.parseTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ShelterImportBatchConfig 유틸리티 메소드 테스트")
class ShelterImportBatchConfigTest {

    @Nested
    @DisplayName("parseTime 메소드 테스트")
    class ParseTimeTest {

        @DisplayName("유효한 시간 형식의 문자열을 LocalTime 객체로 올바르게 변환한다")
        @ParameterizedTest(name = "\"{0}\" 입력 시 LocalTime({1}, {2}) 반환")
        @CsvSource({
                "0900, 9, 0",
                "1830, 18, 30",
                "'0000', 0, 0",
                "2359, 23, 59",
                "'09:00', 9, 0",
                "900, 9, 0"
        })
        void returnsLocalTime_forValidFormats(String input, int hour, int minute) {
            LocalTime result = parseTime(input);

            assertThat(result).isEqualTo(LocalTime.of(hour, minute));
        }

        @DisplayName("null이거나 비어있거나 공백 문자열이 입력되면 null을 반환한다")
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "  "})
        void returnsNull_forNullOrBlankInput(String input) {
            LocalTime result = parseTime(input);

            assertThat(result).isNull();
        }


        @DisplayName("숫자가 아닌 문자열이 입력되면 null을 반환한다")
        @Test
        void returnsNull_forNonDigitInput() {
            LocalTime result = parseTime("알 수 없음");

            assertThat(result).isNull();
        }

        @DisplayName("유효하지 않은 시간 값(예: 25시)이 포함된 문자열이 입력되면 DateTimeParseException 예외를 던진다")
        @ParameterizedTest
        @ValueSource(strings = {"2500", "1299"})
        void throwsException_forInvalidTimeValue(String input) {
            assertThatThrownBy(() -> parseTime(input))
                    .isInstanceOf(DateTimeParseException.class);
        }
    }
}
