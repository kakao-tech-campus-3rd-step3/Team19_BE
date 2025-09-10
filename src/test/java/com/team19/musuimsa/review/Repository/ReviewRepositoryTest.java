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
    void 리뷰_생성_성공() {
        // given
        User user = new User("aran@email.com", "1234", "윤", "image.url");
        userRepository.save(user);

        Shelter shelter = new Shelter(1000L, "임시 쉼터", "address", BigDecimal.TEN,
                BigDecimal.TWO, LocalTime.MAX, LocalTime.NOON, LocalTime.MIDNIGHT, LocalTime.MIN, 5,
                true, 1, 2, 3, 4, "photo.url");

        shelterRepository.save(shelter);

        // when
        CreateReviewRequest request = new CreateReviewRequest("리뷰제목입니다.", "리뷰내용입니다.", 5,
                "phtoo.url");

        Review review = Review.of(shelter, user, request);

        // then
        assertThat(review.getReviewId()).isNull();

        var actual = reviewRepository.save(review);

        assertThat(actual.getReviewId()).isNotNull();
    }

}
