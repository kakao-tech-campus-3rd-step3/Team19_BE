package com.team19.musuimsa.weather.controller;

import com.team19.musuimsa.exception.dto.ErrorResponseDto;
import com.team19.musuimsa.weather.dto.WeatherResponse;
import com.team19.musuimsa.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "날씨 API", description = "현재 위치 기반 날씨 정보 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @Operation(summary = "현재 위치 기온 조회", description = "위도와 경도를 기반으로 가장 최근 관측된 현재 기온을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = WeatherResponse.class),
                            examples = @ExampleObject(name = "현재 기온 조회",
                                    value = "{\"temperature\": 33.5, \"baseDate\": \"20250701\", \"baseTime\": \"1400\"}")
                    )),
            @ApiResponse(responseCode = "400", description = "잘못된 위도/경도 값",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "잘못된 좌표",
                                    value = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"잘못된 위도/경도 값\", \"path\": \"/api/weather/current\"}"))),
            @ApiResponse(responseCode = "502", description = "외부(기상청) API 조회 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(name = "기상청 API 실패",
                                    value = "{\"status\": 502, \"error\": \"Bad Gateway\", \"message\": \"외부 API 호출 실패: https://apihub.kma.go.kr/...\", \"path\": \"/api/weather/current\"}")))
    })
    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> getCurrentTemp(
            @Parameter(description = "현재 위도", example = "37.5665", required = true)
            @RequestParam double latitude,
            @Parameter(description = "현재 경도", example = "126.9780", required = true)
            @RequestParam double longitude) {
        return ResponseEntity.ok(weatherService.getCurrentTemp(latitude, longitude));
    }
}