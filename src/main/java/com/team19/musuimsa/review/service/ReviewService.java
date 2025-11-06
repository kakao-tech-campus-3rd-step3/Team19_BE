package com.team19.musuimsa.review.service;

import com.team19.musuimsa.exception.conflict.OptimisticLockConflictException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private static final int MAX_RETRY = 3;

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
        refreshShelterStatsWithRetry(shelter);

        return ReviewResponse.from(review);
    }

    // 리뷰 수정
    public ReviewResponse updateReview(Long reviewId, UpdateReviewRequest request, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));

        review.assertOwnedBy(user);

        review.update(request.content(), request.rating(), request.photoUrl());
        refreshShelterStatsWithRetry(review.getShelter());

        return ReviewResponse.from(review);
    }

    // 리뷰 삭제
    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId).orElseThrow(
                () -> new ReviewNotFoundException(reviewId));

        review.assertOwnedBy(user);

        reviewRepository.delete(review);
        refreshShelterStatsWithRetry(review.getShelter());
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

        // 내부에서 즉시 충돌 감지
        shelterRepository.saveAndFlush(shelter);
    }

    private void refreshShelterStatsWithRetry(Shelter shelter) {
        for (int i = 0; i < MAX_RETRY; i++) {
            try {
                updateReviewsOfShelter(shelter);
                return;
            } catch (OptimisticLockingFailureException e) {
                if (i == MAX_RETRY - 1) {
                    throw new OptimisticLockConflictException();
                }

                // 짧은 지수 백오프(20ms, 40ms, 80ms)
                try {
                    Thread.sleep((1L << i) * 20L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                Long id = shelter.getShelterId();
                shelter = shelterRepository.findById(id)
                        .orElseThrow(() -> new ShelterNotFoundException(id));
            }
        }
    }

    @Transactional(readOnly = true)
    public Review getReviewEntity(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    }

    public ReviewResponse updateReviewPhotoUrl(Long reviewId, String newPhotoUrl, User loginUser) {
        Review review = getReviewEntity(reviewId);
        review.assertOwnedBy(loginUser);

        review.updatePhotoUrl(newPhotoUrl);

        return ReviewResponse.from(review);
    }
}
