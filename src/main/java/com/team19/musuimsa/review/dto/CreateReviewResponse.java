package com.team19.musuimsa.review.dto;

import com.team19.musuimsa.review.domain.Review;
import java.time.LocalDateTime;

public record CreateReviewResponse(
        Long reviewId,
        Long shelterId,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CreateReviewResponse from(Review review) {
        return new CreateReviewResponse(
                review.getReviewId(), review.getShelter().getShelterId(),
                review.getUser().getUserId(),
                review.getCreatedAt(), review.getUpdatedAt()
        );
    }
}
