package com.team19.musuimsa.review.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.team19.musuimsa.review.dto.CreateReviewRequest;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
public class ReviewTest {

    @Autowired
    private UserRepository userRepository;

    private Shelter shelter;
    private User user;

    @BeforeEach
    void setup() {
        shelter = new Shelter(10L, "무더위쉼터", "충대정문앞", BigDecimal.TEN, BigDecimal.TWO, LocalTime.MAX,
                LocalTime.MIDNIGHT, LocalTime.MIN, LocalTime.NOON, 50, false, 10, 3, 4, 5,
                "photo.url");

        user = userRepository.save(new User("aran@email.com", "1234", "별명", "프사.url"));

        // SecurityContext에 수동으로 User 객체 주입
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user,
                null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

    }

    @Test
    @DisplayName("리뷰 객체 생성 성공")
    void reviewCreateSuccess() {
        // given
        String content = "시원해요";
        int rating = 5;
        String imageUrl = "사진.com";

        CreateReviewRequest request = new CreateReviewRequest(content, rating, imageUrl);

        // when
        Review review = Review.of(shelter, user, request);

        // then
        assertThat(review).isNotNull();
        assertThat(review.getShelter()).isEqualTo(shelter);
        assertThat(review.getUser().getUserId()).isEqualTo(user.getUserId());
        assertThat(review.getContent()).isEqualTo(content);
        assertThat(review.getRating()).isEqualTo(rating);
        assertThat(review.getPhotoUrl()).isEqualTo(imageUrl);
    }

    @Test
    @DisplayName("리뷰 수정 성공")
    void reviewUpdateSuccess() {
        // given
        CreateReviewRequest request = new CreateReviewRequest("수정 전 리뷰입니다.", 5, "수정 전 이미지");
        Review review = Review.of(shelter, user, request);

        String updatedContent = "수정 후 리뷰입니다.";
        int updatedRating = 1;

        // when
        review.update(updatedContent, updatedRating, null);

        // then
        assertThat(review.getContent()).isEqualTo(updatedContent);
        assertThat(review.getRating()).isEqualTo(updatedRating);
        assertThat(review.getPhotoUrl()).isEqualTo("수정 전 이미지");
    }
}
