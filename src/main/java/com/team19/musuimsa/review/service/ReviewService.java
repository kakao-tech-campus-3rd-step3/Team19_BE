package com.team19.musuimsa.review.service;

import com.team19.musuimsa.exception.notfound.ReviewNotFoundException;
import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.exception.notfound.UserNotFoundException;
import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.review.dto.ShelterReviewCountAndSum;
import com.team19.musuimsa.review.dto.UpdateReviewRequest;
import com.team19.musuimsa.review.repository.ReviewRepository;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
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

        updateReviewsOfShelter(shelter);

        return ReviewResponse.from(review);
    }

    // 리뷰 수정
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        review.assertOwnedBy(user);

        review.update(request.content(), request.rating(), request.photoUrl());

        Long shelterId = review.getShelter().getShelterId();

        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        updateReviewsOfShelter(shelter);

        return ReviewResponse.from(review);
    }

    // 리뷰 삭제
    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ReviewNotFoundException(reviewId));

        review.assertOwnedBy(user);

        reviewRepository.delete(review);

        Long shelterId = review.getShelter().getShelterId();

        Shelter shelter = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        updateReviewsOfShelter(shelter);
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
        // shelter 존재 검증은 유지
        shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        return reviewRepository.findByShelterIdWithShelterName(shelterId);
    }

    // 사용자별 리뷰 조회
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByUser(User user) {
        User loginUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new UserNotFoundException(user.getUserId()));

        return reviewRepository.findByUserIdWithShelterName(loginUser.getUserId());
    }
    
    private void updateReviewsOfShelter(Shelter shelter) {
        ShelterReviewCountAndSum dto = reviewRepository.aggregateByShelterId(
                shelter.getShelterId());

        int newCount = Math.toIntExact(dto.reviewCount());
        int newSum = Math.toIntExact(dto.totalRating());

        if (!Objects.equals(shelter.getReviewCount(), newCount)) {
            shelter.updateReviewCount(newCount);
        }
        if (!Objects.equals(shelter.getTotalRating(), newSum)) {
            shelter.updateTotalRating(newSum);
        }
    }
}
