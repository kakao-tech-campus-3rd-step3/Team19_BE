package com.team19.musuimsa.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "리뷰 수정 요청 데이터")
public record UpdateReviewRequest(
        @NotBlank @Size(max = 100, message = "리뷰는 100자까지 작성 가능합니다.")
        @Schema(description = "리뷰 내용", example = "정말 좋아요!")
        String content,

        @NotNull @Min(1) @Max(5)
        @Schema(description = "리뷰 별점", example = "4")
        int rating,

        @Schema(description = "리뷰 사진 URL", example = "http://example.com/photo.jpg")
        String photoUrl
) {

}
