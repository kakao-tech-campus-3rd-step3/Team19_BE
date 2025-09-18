package com.team19.musuimsa.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateReviewRequest(
        @NotBlank String content,
        @NotNull @Min(1) @Max(5) int rating,
        String photoUrl
) {

}
