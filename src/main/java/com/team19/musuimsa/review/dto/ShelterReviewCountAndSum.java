package com.team19.musuimsa.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쉼터 리뷰 개수 및 총 별점 데이터")
public record ShelterReviewCountAndSum(
        @Schema(description = "리뷰 개수", example = "15")
        long reviewCount,

        @Schema(description = "총 평점", example = "65")
        long totalRating
) {

}