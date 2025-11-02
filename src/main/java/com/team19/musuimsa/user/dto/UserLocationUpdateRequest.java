package com.team19.musuimsa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "사용자 위치 업데이트 요청 데이터")
public record UserLocationUpdateRequest(
        @Schema(description = "위도", example = "37.7749")
        BigDecimal latitude,
        @Schema(description = "경도", example = "-122.4194")
        BigDecimal longitude
) {

}