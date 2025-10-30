package com.team19.musuimsa.review.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "리뷰 생성 요청 데이터")
public record CreateReviewRequest(
        @NotBlank
        @Schema(description = "리뷰 내용", example = "여기 정말 시원해요!", requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @NotNull @Min(1) @Max(5)
        @Schema(description = "별점 (1~5)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        int rating,
        
        @Schema(description = "리뷰 사진 URL", example = "https://example.com/review.jpg")
        String photoUrl
) {

}
