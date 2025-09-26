package com.team19.musuimsa.review.dto;

import com.team19.musuimsa.review.domain.Review;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long reviewId,
        Long shelterId,
        String shelterName,
        Long userId,
        String nickname,
        String content,
        int rating,
        String photoUrl,
        String profileImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getReviewId(), review.getShelter().getShelterId(),
                review.getShelter().getName(),
                review.getUser().getUserId(), review.getUser().getNickname(),
                review.getContent(), review.getRating(), review.getPhotoUrl(),
                review.getUser().getProfileImageUrl(),
                review.getCreatedAt(), review.getUpdatedAt()
        );
    }
}
