package com.team19.musuimsa.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@Schema(description = "API 오류 응답 형식")
public record ErrorResponseDto(
        @Schema(description = "HTTP 상태 코드", example = "404")
        int status,
        @Schema(description = "HTTP 상태 메시지", example = "Not Found")
        String error,
        @Schema(description = "오류 상세 메시지", example = "해당 ID의 사용자를 찾을 수 없습니다: 999")
        String message,
        @Schema(description = "오류가 발생한 요청 경로", example = "/api/users/999")
        String path
) {

    public static ErrorResponseDto from(
            HttpStatus status,
            String message,
            String path) {

        return new ErrorResponseDto(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
