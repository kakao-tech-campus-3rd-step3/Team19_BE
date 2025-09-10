package com.team19.musuimsa.review.controller;

import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.review.dto.UpdateReviewRequest;
import com.team19.musuimsa.review.service.ReviewService;
import com.team19.musuimsa.user.domain.User;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    // 리뷰 생성
    @PostMapping("/shelters/{shelterId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @RequestBody CreateReviewRequest request, @PathVariable Long shelterId,
            @AuthenticationPrincipal User user) {

        ReviewResponse response = reviewService.createReview(shelterId, request, user);

        URI location = URI.create("/api/reviews/" + response.reviewId());

        return ResponseEntity.created(location).body(response);
    }

    // 리뷰 수정
    @PatchMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(@PathVariable Long reviewId,
            @RequestBody UpdateReviewRequest request, @AuthenticationPrincipal User user)
            throws AccessDeniedException {

        ReviewResponse response = reviewService.updateReview(reviewId, request, user);

        return ResponseEntity.ok(response);
    }

    // 리뷰 삭제
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId,
            @AuthenticationPrincipal User user)
            throws AccessDeniedException {

        reviewService.deleteReview(reviewId, user);

        return ResponseEntity.noContent().build();
    }

    // 리뷰 단건 조회
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long reviewId) {

        ReviewResponse response = reviewService.getReview(reviewId);

        return ResponseEntity.ok(response);
    }

    // 쉼터 리뷰 조회
    @GetMapping("/shelters/{shelterId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviewByShelter(@PathVariable Long shelterId) {

        List<ReviewResponse> reviews = reviewService.getReviewsByShelter(shelterId);

        return ResponseEntity.ok(reviews);
    }

    // 내가 쓴 리뷰 조회
    @GetMapping("/users/me/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviewByUser(
            @AuthenticationPrincipal User user) {

        List<ReviewResponse> reviews = reviewService.getReviewsByUser(user);

        return ResponseEntity.ok(reviews);
    }
}
