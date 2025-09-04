package com.team19.musuimsa.review.dto;

import com.team19.musuimsa.user.domain.User;
import java.time.LocalDateTime;

public record CreateReviewResponse(
        Long reviewId,
        Long shelterId,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

}
