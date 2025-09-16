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
import com.team19.musuimsa.user.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
@Transactional
public class ReviewServiceTest {

    @Mock
    ReviewRepository reviewRepository;
    @Mock
    ShelterRepository shelterRepository;
    @Mock
    UserRepository userRepository;

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

        shelter = new Shelter(shelterId, "무더위쉼터", "충대정문앞", BigDecimal.TEN, BigDecimal.TWO,
                LocalTime.MAX,
                LocalTime.MIDNIGHT, LocalTime.MIN, LocalTime.NOON, 50, false, 10, 3, 4, 5,
                "photo.url");

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

        verify(shelterRepository, times(2)).findById(
                shelterId);  // create 과정에서: (1) 저장용 findById, (2) 집계 갱신용 findById → 총 2회
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
        given(shelterRepository.findById(eq(shelterId))).willReturn(Optional.of(shelter));

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
    @DisplayName("리뷰 삭제 실패 - 403 반환")
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
        given(shelterRepository.findById(eq(shelterId))).willReturn(Optional.of(shelter));
        given(reviewRepository.aggregateByShelterId(eq(shelterId)))
                .willReturn(new ShelterReviewCountAndSum(count, sum));
    }
}
