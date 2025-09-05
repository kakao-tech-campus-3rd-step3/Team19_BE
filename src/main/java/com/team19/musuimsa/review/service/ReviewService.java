package com.team19.musuimsa.review.service;

import com.team19.musuimsa.exception.notfound.ReviewNotFoundException;
import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.exception.notfound.UserNotFoundException;
import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.review.dto.CreateReviewResponse;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.review.dto.UpdateReviewRequest;
import com.team19.musuimsa.review.repository.ReviewRepository;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import java.nio.file.AccessDeniedException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ShelterRepository shelterRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, ShelterRepository shelterRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.shelterRepository = shelterRepository;
        this.userRepository = userRepository;
    }

    // 리뷰 생성
    @Transactional
    public CreateReviewResponse createReview(Long shelterId, CreateReviewRequest request, User user) {
        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        Review review = Review.of(shelter, user, request.photoUrl(), request.title(),
                request.content(), request.rating());

        reviewRepository.save(review);

        CreateReviewResponse response = new CreateReviewResponse(review.getReviewId(),
                review.getShelter().getShelterId(), review.getUser().getUserId(),
                review.getCreatedAt(), review.getUpdatedAt());

        return response;
    }

    // 리뷰 수정
    @Transactional
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, User user)
            throws AccessDeniedException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        if (!review.getUser().equals(user)) {
            throw new AccessDeniedException("자신의 리뷰만 수정할 수 있습니다.");
        }

        review.update(request.title(), request.content(), request.rating(), request.photoUrl());

        return toDto(review);
    }

    // 리뷰 삭제
    public void deleteReview(Long reviewId, User user)
            throws AccessDeniedException {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ReviewNotFoundException(reviewId));

        if (!review.getUser().equals(user)) {
            throw new AccessDeniedException("자신의 리뷰만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
    }

    // 쉼터별 리뷰 조회
    public List<ReviewResponse> getReviewsByShelter(Long shelterId) {
        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        List<Review> reviews = reviewRepository.findByShelterOrderByCreatedAtDesc(shelter);

        return reviews.stream()
                .map(this::toDto)
                .toList();
    }

    // 사용자별 리뷰 조회
    public List<ReviewResponse> getReviewsByUser(User user) {
        User loginUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new UserNotFoundException(user.getUserId()));

        List<Review> reviews = reviewRepository.findByUser(loginUser);

        return reviews.stream()
                .map(this::toDto)
                .toList();
    }

    private ReviewResponse toDto(Review review) {
        return new ReviewResponse(
                review.getReviewId(), review.getShelter().getShelterId(),
                review.getUser().getUserId(), review.getUser().getNickname(), review.getTitle(),
                review.getContent(), review.getRating(), review.getPhotoUrl(),
                review.getCreatedAt(), review.getUpdatedAt()
        );
    }
}
