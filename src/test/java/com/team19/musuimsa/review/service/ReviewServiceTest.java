package com.team19.musuimsa.review.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team19.musuimsa.exception.conflict.OptimisticLockConflictException;
import com.team19.musuimsa.exception.forbidden.ReviewAccessDeniedException;
import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.review.dto.ReviewResponse;
import com.team19.musuimsa.review.dto.ShelterReviewCountAndSum;
import com.team19.musuimsa.review.dto.UpdateReviewRequest;
import com.team19.musuimsa.review.repository.ReviewRepository;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@Transactional
public class ReviewServiceTest {

    @Mock
    ReviewRepository reviewRepository;
    @Mock
    ShelterRepository shelterRepository;

    @InjectMocks
    ReviewService reviewService;

    private Shelter shelter;
    private User user;
    private Review review;
    private Long shelterId;
    private Long reviewId;

    @BeforeEach
    void setup() {
        shelterId = 10L;
        reviewId = 1L;

        shelter = Shelter.builder()
                .shelterId(shelterId)
                .name("무더위쉼터")
                .address("충대정문앞")
                .latitude(BigDecimal.TEN)
                .longitude(BigDecimal.TWO)
                .weekdayOpenTime(LocalTime.MAX)
                .weekdayCloseTime(LocalTime.MIDNIGHT)
                .weekendOpenTime(LocalTime.MIN)
                .weekendCloseTime(LocalTime.NOON)
                .capacity(50)
                .isOutdoors(false)
                .fanCount(10)
                .airConditionerCount(3)
                .totalRating(4)
                .reviewCount(5)
                .photoUrl("photo.url")
                .build();

        user = new User("aran@email.com", "1234", "별명", "프사.url");
    }

    @Test
    @DisplayName("리뷰 작성 성공")
    void createReviewSuccess() {
        // given
        CreateReviewRequest request = new CreateReviewRequest("시원하네요", 5, "리뷰사진");
        review = Review.of(shelter, user, request);

        given(shelterRepository.findById(any(Long.class))).willReturn(Optional.of(shelter));
        given(reviewRepository.save(any(Review.class))).willReturn(review);

        stubAggregateForShelter(1L, 5L);

        // when
        ReviewResponse response = reviewService.createReview(shelterId, request, user);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(request.content());
        assertThat(response.rating()).isEqualTo(request.rating());

        verify(shelterRepository, times(1)).findById(shelterId);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).aggregateByShelterId(eq(shelterId));
    }

    @Test
    @DisplayName("리뷰 내용, url 없이도 작성 성공")
    void createReviewWithNoContentSuccess() {
        // given
        CreateReviewRequest request = new CreateReviewRequest("", 5, "");
        review = Review.of(shelter, user, request);

        given(shelterRepository.findById(any(Long.class))).willReturn(Optional.of(shelter));
        given(reviewRepository.save(any(Review.class))).willReturn(review);

        stubAggregateForShelter(1L, 5L);

        // when
        ReviewResponse response = reviewService.createReview(shelterId, request, user);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(request.content());
        assertThat(response.rating()).isEqualTo(request.rating());

        verify(shelterRepository, times(1)).findById(shelterId);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewRepository).aggregateByShelterId(eq(shelterId));
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void updateReviewSuccess() throws ReviewAccessDeniedException {
        // given
        Long id = 1L;
        CreateReviewRequest request = new CreateReviewRequest("시원하네요", 5, "리뷰사진");
        review = Review.of(shelter, user, request);
        Review reviewSpy = spy(review);

        doNothing().when(reviewSpy).assertOwnedBy(user);  // 소유자 검증 패스

        UpdateReviewRequest updateRequest = new UpdateReviewRequest("생각해보니 별로 안 시원해서 1점 드립니다.", 1,
                "수정된 사진");

        given(reviewRepository.findById(eq(id))).willReturn(Optional.of(reviewSpy));

        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(1L, 1L));      // 수정 후 리뷰 개수1, 총점 1

        // when
        ReviewResponse response = reviewService.updateReview(id, updateRequest, user);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(updateRequest.content());
        assertThat(response.rating()).isEqualTo(updateRequest.rating());

        verify(reviewRepository).findById(eq(id));
        verify(reviewRepository).aggregateByShelterId(eq(shelterId));
    }

    @Test
    @DisplayName("리뷰 내용 없애기 수정 성공")
    void updateReviewWithNoContentSuccess() throws ReviewAccessDeniedException {
        // given
        Long id = 1L;
        CreateReviewRequest request = new CreateReviewRequest("시원하네요", 5, "리뷰사진");
        review = Review.of(shelter, user, request);
        Review reviewSpy = spy(review);

        doNothing().when(reviewSpy).assertOwnedBy(user);  // 소유자 검증 패스

        UpdateReviewRequest updateRequest = new UpdateReviewRequest("", 1,
                "");

        given(reviewRepository.findById(eq(id))).willReturn(Optional.of(reviewSpy));

        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(1L, 1L));      // 수정 후 리뷰 개수1, 총점 1

        // when
        ReviewResponse response = reviewService.updateReview(id, updateRequest, user);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(updateRequest.content());
        assertThat(response.rating()).isEqualTo(updateRequest.rating());

        verify(reviewRepository).findById(eq(id));
        verify(reviewRepository).aggregateByShelterId(eq(shelterId));
    }

    @Test
    @DisplayName("내용 없던 리뷰에 내용 추가해서 수정 성공")
    void updateNoContentReviewSuccess() throws ReviewAccessDeniedException {
        // given
        Long id = 1L;
        CreateReviewRequest request = new CreateReviewRequest("", 5,
                null);
        review = Review.of(shelter, user, request);
        Review reviewSpy = spy(review);

        doNothing().when(reviewSpy).assertOwnedBy(user);  // 소유자 검증 패스

        UpdateReviewRequest updateRequest = new UpdateReviewRequest("시원합니다.", 1, "photoUrl");

        given(reviewRepository.findById(eq(id))).willReturn(Optional.of(reviewSpy));

        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(1L, 1L));      // 수정 후 리뷰 개수 1, 총점 1

        // when
        ReviewResponse response = reviewService.updateReview(id, updateRequest, user);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(updateRequest.content());
        assertThat(response.rating()).isEqualTo(updateRequest.rating());

        verify(reviewRepository).findById(eq(id));
        verify(reviewRepository).aggregateByShelterId(eq(shelterId));
    }

    @Test
    @DisplayName("별점만 수정 성공")
    void updateReviewRatingSuccess() throws ReviewAccessDeniedException {
        // given
        Long id = 1L;
        CreateReviewRequest request = new CreateReviewRequest("", 5,
                "");
        review = Review.of(shelter, user, request);
        Review reviewSpy = spy(review);

        doNothing().when(reviewSpy).assertOwnedBy(user);  // 소유자 검증 패스

        UpdateReviewRequest updateRequest = new UpdateReviewRequest(null, 1, null);

        given(reviewRepository.findById(eq(id))).willReturn(Optional.of(reviewSpy));

        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(1L, 1L));      // 수정 후 리뷰 개수 1, 총점 1

        // when
        ReviewResponse response = reviewService.updateReview(id, updateRequest, user);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(
                request.content());    // content는 값 변경 없었으므로 원래 요청 시의 값과 비교
        assertThat(response.rating()).isEqualTo(updateRequest.rating());
        assertThat(response.photoUrl()).isEqualTo(
                request.photoUrl());   // photoUrl도 값 변경 없었으므로 원래 요청 시의 값과 비교

        verify(reviewRepository).findById(eq(id));
        verify(reviewRepository).aggregateByShelterId(eq(shelterId));
    }

    @Test
    @DisplayName("내용만 수정 성공")
    void updateReviewContentSuccess() throws ReviewAccessDeniedException {
        // given
        Long id = 1L;
        CreateReviewRequest request = new CreateReviewRequest("", 5,
                "");
        review = Review.of(shelter, user, request);
        Review reviewSpy = spy(review);

        doNothing().when(reviewSpy).assertOwnedBy(user);  // 소유자 검증 패스

        UpdateReviewRequest updateRequest = new UpdateReviewRequest("수정된 내용입니다.", null, null);

        given(reviewRepository.findById(eq(id))).willReturn(Optional.of(reviewSpy));

        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(1L, 1L));      // 수정 후 리뷰 개수 1, 총점 1

        // when
        ReviewResponse response = reviewService.updateReview(id, updateRequest, user);

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo(updateRequest.content());
        assertThat(response.rating()).isEqualTo(
                request.rating());  // rating은 값 변경 없었으므로 원래 요청인 request의 값과 비교
        assertThat(response.photoUrl()).isEqualTo(
                request.photoUrl());   // photoUrl는 값 변경 없었으므로 원래 요청 시의 값과 비교

        verify(reviewRepository).findById(eq(id));
        verify(reviewRepository).aggregateByShelterId(eq(shelterId));
    }

    @Test
    @DisplayName("작성자가 아니면 리뷰 삭제 불가 - 403 반환")
    void deleteReviewFail() throws ReviewAccessDeniedException {
        // given
        CreateReviewRequest request = new CreateReviewRequest("시원하네요", 5, "리뷰사진");
        review = Review.of(shelter, user, request);
        Review reviewSpy = spy(review);

        User other = new User("other@email.com", "1234", "Not 작성자", "profile.image");

        doThrow(new ReviewAccessDeniedException())
                .when(reviewSpy).assertOwnedBy(other);

        given(reviewRepository.findById(eq(reviewId))).willReturn(Optional.of(reviewSpy));

        // when & then
        assertThatThrownBy(() -> reviewService.deleteReview(reviewId, other))
                .isInstanceOf(ReviewAccessDeniedException.class);

        verify(reviewRepository).findById(eq(reviewId));
    }

    // 집계(count, sum)를 한 번에 스텁한다.
    private void stubAggregateForShelter(long count, long sum) {
        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(count, sum));
    }

    @Test
    @DisplayName("낙관적 락 충돌 발생 시 재시도 후 성공")
    void optimisticLock_retry_then_success() {
        // given
        CreateReviewRequest request = new CreateReviewRequest("좋아요", 5, "img");
        review = Review.of(shelter, user, request);

        given(shelterRepository.findById(eq(shelterId))).willReturn(Optional.of(shelter));
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(1L, 5L));

        // saveAndFlush: 1,2회는 충돌 → 3회째 성공
        given(shelterRepository.saveAndFlush(any(Shelter.class)))
                .willThrow(new ObjectOptimisticLockingFailureException(Shelter.class, shelterId))
                .willThrow(new ObjectOptimisticLockingFailureException(Shelter.class, shelterId))
                .willReturn(shelter);

        // when
        ReviewResponse resp = reviewService.createReview(shelterId, request, user);

        // then
        assertThat(resp).isNotNull();
        assertThat(resp.content()).isEqualTo("좋아요");
        assertThat(resp.rating()).isEqualTo(5);

        // saveAndFlush가 총 3회 호출되었는지(2회 실패 + 1회 성공)
        verify(shelterRepository, times(3)).saveAndFlush(any(Shelter.class));
        verify(reviewRepository, times(3)).aggregateByShelterId(eq(shelterId));
    }

    @Test
    @DisplayName("낙관적 락 3회 충돌 시 OptimisticLockConflictException 발생")
    void optimisticLock_retry_exhausted_then_fail() {
        // given
        CreateReviewRequest request = new CreateReviewRequest("좋아요", 5, "img");
        review = Review.of(shelter, user, request);

        given(shelterRepository.findById(eq(shelterId))).willReturn(Optional.of(shelter));
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(1L, 5L));

        // 3회 모두 충돌
        given(shelterRepository.saveAndFlush(any(Shelter.class)))
                .willThrow(new ObjectOptimisticLockingFailureException(Shelter.class, shelterId))
                .willThrow(new ObjectOptimisticLockingFailureException(Shelter.class, shelterId))
                .willThrow(new ObjectOptimisticLockingFailureException(Shelter.class, shelterId));

        // when & then
        assertThatThrownBy(() -> reviewService.createReview(shelterId, request, user))
                .isInstanceOf(OptimisticLockConflictException.class);

        verify(shelterRepository, times(3)).saveAndFlush(any(Shelter.class));
        verify(reviewRepository, times(3)).aggregateByShelterId(eq(shelterId));
    }

}
