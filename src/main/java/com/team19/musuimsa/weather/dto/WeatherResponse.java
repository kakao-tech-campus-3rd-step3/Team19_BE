package com.team19.musuimsa.weather.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 날씨(기온) 응답")
public record WeatherResponse(
        @Schema(description = "현재 기온(°C)", example = "33.5")
        double temperature,
        @Schema(description = "기준 날짜(YYYYMMDD)", example = "20230605")
        String baseDate,
        @Schema(description = "기준 시간(HHMM)", example = "1400")
        String baseTime
) {

}