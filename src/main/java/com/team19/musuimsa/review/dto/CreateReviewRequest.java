package com.team19.musuimsa.review.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "리뷰 생성 요청 데이터")
public record CreateReviewRequest(
        @Size(max = 100, message = "리뷰는 100자까지 작성 가능합니다.")
        @Schema(description = "리뷰 내용", example = "여기 정말 시원해요!", requiredMode = Schema.RequiredMode.REQUIRED)
        String content,

        @NotNull @Min(1) @Max(5)
        @Schema(description = "별점 (1~5)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer rating,

        @Schema(description = "리뷰 사진 URL", example = "https://example.com/review.jpg")
        String photoUrl
) {

}
