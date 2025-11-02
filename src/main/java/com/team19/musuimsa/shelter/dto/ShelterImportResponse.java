package com.team19.musuimsa.shelter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자: 쉼터 데이터 수동 임포트 응답")
public record ShelterImportResponse(
        @Schema(description = "총 데이터 개수", example = "150")
        int saved
) {

}
