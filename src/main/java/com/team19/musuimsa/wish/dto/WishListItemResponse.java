package com.team19.musuimsa.wish.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "위시리스트 아이템 응답 데이터")
public record WishListItemResponse(
        @Schema(description = "쉼터 ID", example = "1")
        Long shelterId,
        @Schema(description = "쉼터 이름", example = "행복한 쉼터")
        String name,
        @Schema(description = "쉼터 주소", example = "서울특별시 강남구 테헤란로 123")
        String address,
        @Schema(description = "운영 시간", example = "09:00 ~ 18:00")
        String operatingHours,
        @Schema(description = "평균 별점", example = "4.5")
        Double averageRating,
        @Schema(description = "쉼터 사진 URL", example = "http://example.com/photo.jpg")
        String photoUrl,
        @Schema(description = "쉼터까지의 거리", example = "2.5km")
        String distance
) {

}
