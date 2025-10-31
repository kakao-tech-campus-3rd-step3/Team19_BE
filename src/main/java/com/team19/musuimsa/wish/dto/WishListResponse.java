package com.team19.musuimsa.wish.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "위시리스트 응답 데이터")
public record WishListResponse(
        @Schema(description = "위시리스트 아이템 목록", example = "[]")
        List<WishListItemResponse> items
) {

}
