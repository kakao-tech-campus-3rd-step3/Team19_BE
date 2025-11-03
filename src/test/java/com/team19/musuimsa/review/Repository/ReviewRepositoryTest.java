package com.team19.musuimsa.review.Repository;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.team19.musuimsa.review.domain.Review;
import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.review.repository.ReviewRepository;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@DataJpaTest
@EnableJpaAuditing
public class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("리뷰 생성 성공")
    void createReviewSuccess() {
        // given
        User user = new User("aran@email.com", "1234", "윤", "image.url");
        userRepository.save(user);

        Shelter shelter = Shelter.builder()
                .shelterId(1000L)
                .name("임시 쉼터")
                .address("address")
                .latitude(BigDecimal.TEN)
                .longitude(BigDecimal.TWO)
                .weekdayOpenTime(LocalTime.MAX)
                .weekdayCloseTime(LocalTime.MIDNIGHT)
                .weekendOpenTime(LocalTime.MIN)
                .capacity(5)
                .isOutdoors(true)
                .fanCount(1)
                .airConditionerCount(2)
                .totalRating(3)
                .reviewCount(4)
                .photoUrl("photo.url")
                .build();

        shelterRepository.save(shelter);

        // when
        CreateReviewRequest request = new CreateReviewRequest("리뷰제목입니다.", 5, "photo.url");

        Review review = Review.of(shelter, user, request);

        // then
        assertThat(review.getReviewId()).isNull();

        Review actual = reviewRepository.save(review);

        assertThat(actual.getReviewId()).isNotNull();
    }

}
