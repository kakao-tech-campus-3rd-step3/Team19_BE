package com.team19.musuimsa.review.dto;

import com.team19.musuimsa.review.domain.Review;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "리뷰 정보 응답 데이터")
public record ReviewResponse(
        @Schema(description = "리뷰 ID", example = "1")
        Long reviewId,
        @Schema(description = "쉼터 ID", example = "1")
        Long shelterId,
        @Schema(description = "쉼터 이름", example = "행복한 쉼터")
        String shelterName,
        @Schema(description = "작성자 ID", example = "1")
        Long userId,
        @Schema(description = "작성자 닉네임", example = "리뷰왕")
        String nickname,
        @Schema(description = "리뷰 내용", example = "여기 정말 시원해요!")
        String content,
        @Schema(description = "리뷰 별점", example = "5")
        int rating,
        @Schema(description = "리뷰 사진 URL", example = "https://example.com/review.jpg")
        String photoUrl,
        @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl,
        @Schema(description = "리뷰 작성일시", example = "2024-01-01T12:00:00")
        LocalDateTime createdAt,
        @Schema(description = "리뷰 수정일시", example = "2024-01-02T12:00:00")
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

    // 리뷰 사진 URL 업데이트
    public ReviewResponse withPhotoUrl(String newPhotoUrl) {
        return new ReviewResponse(
                this.reviewId, this.shelterId, this.shelterName, this.userId, this.nickname,
                this.content, this.rating, newPhotoUrl, this.profileImageUrl, this.createdAt, this.updatedAt
        );
    }

    // 작성자 프로필 URL 업데이트
    public ReviewResponse withProfileImageUrl(String newProfileImageUrl) {
        return new ReviewResponse(
                this.reviewId, this.shelterId, this.shelterName, this.userId, this.nickname,
                this.content, this.rating, this.photoUrl, newProfileImageUrl, this.createdAt, this.updatedAt
        );
    }
}
