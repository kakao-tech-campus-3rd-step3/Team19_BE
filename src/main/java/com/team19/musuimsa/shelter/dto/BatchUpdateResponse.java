package com.team19.musuimsa.shelter.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자: 배치 작업 결과 응답")
public record BatchUpdateResponse(
        @Schema(description = "처리된 항목 수", example = "100")
        int processed,
        @Schema(description = "업데이트된 항목 수", example = "90")
        int updated,
        @Schema(description = "실패한 항목 수", example = "10")
        int failed
) {

}
