package com.team19.musuimsa.review.dto;

import com.team19.musuimsa.review.domain.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long shelterId,
        Long userId,
        String nickname,
        String title,
        String content,
        int rating,
        String photoUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReviewId(), review.getShelter().getShelterId(),
                review.getUser().getUserId(), review.getUser().getNickname(), review.getTitle(),
                review.getContent(), review.getRating(), review.getPhotoUrl(),
                review.getCreatedAt(), review.getUpdatedAt()
        );
    }
}
