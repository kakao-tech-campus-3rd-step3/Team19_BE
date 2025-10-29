package com.team19.musuimsa.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
        @NotBlank @Size(max = 100, message = "리뷰는 100자까지 작성 가능합니다.") String content,
        @NotNull @Min(1) @Max(5) int rating,
        String photoUrl
) {

}
