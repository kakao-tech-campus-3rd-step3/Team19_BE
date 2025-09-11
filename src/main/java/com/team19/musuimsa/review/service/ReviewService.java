package com.team19.musuimsa.review.service;

import com.team19.musuimsa.exception.forbidden.UserAccessDeniedException;
import com.team19.musuimsa.exception.notfound.ReviewNotFoundException;
import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.exception.notfound.UserNotFoundException;
import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.review.dto.UpdateReviewRequest;
import com.team19.musuimsa.review.repository.ReviewRepository;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ShelterRepository shelterRepository;
    private final UserRepository userRepository;

    // 리뷰 생성
    public ReviewResponse createReview(Long shelterId, CreateReviewRequest request,
            User user) {
        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        Review review = Review.of(shelter, user, request);

        reviewRepository.save(review);

        return ReviewResponse.from(review);
    }

    // 리뷰 수정
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, User user)
            throws UserAccessDeniedException {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        validateWriter(review, user);

        review.update(request.content(), request.rating(), request.photoUrl());

        return ReviewResponse.from(review);
    }

    // 리뷰 삭제
    public void deleteReview(Long reviewId, User user)
            throws UserAccessDeniedException {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ReviewNotFoundException(reviewId));

        validateWriter(review, user);

        reviewRepository.delete(review);
    }

    // 리뷰 단건 조회
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {

        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ReviewNotFoundException(reviewId)
        );

        return ReviewResponse.from(review);
    }

    // 쉼터별 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByShelter(Long shelterId) {
        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        List<Review> reviews = reviewRepository.findByShelterOrderByCreatedAtDesc(shelter);

        return reviews.stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // 사용자별 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(User user) {
        User loginUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new UserNotFoundException(user.getUserId()));

        List<Review> reviews = reviewRepository.findByUser(loginUser);

        return reviews.stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // 리뷰에 접근하려는 사용자 검증
    private void validateWriter(Review review, User user) throws UserAccessDeniedException {
        if (!review.isWriter(user)) {
            throw new UserAccessDeniedException("본인의 리뷰에만 접근할 수 있습니다.");
        }
    }

}
