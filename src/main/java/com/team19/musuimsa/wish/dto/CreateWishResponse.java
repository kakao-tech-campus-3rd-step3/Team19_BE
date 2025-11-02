package com.team19.musuimsa.wish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "위시 생성 응답 데이터")
public record CreateWishResponse(
        @Schema(description = "위시 ID", example = "1")
        Long wishId,
        @Schema(description = "사용자 ID", example = "1")
        Long userId,
        @Schema(description = "쉼터 ID", example = "1")
        Long shelterId,
        @Schema(description = "생성 일시", example = "2024-06-15T14:30:00")
        LocalDateTime createdAt
) {

}
