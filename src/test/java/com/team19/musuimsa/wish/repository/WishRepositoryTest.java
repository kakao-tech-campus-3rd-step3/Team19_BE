package com.team19.musuimsa.wish.repository;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.wish.domain.Wish;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DataJpaTest
class WishRepositoryTest {

    @Autowired
    WishRepository wishRepository;

    @Autowired
    EntityManager entityManager;

    @Test
    @DisplayName("userId + shelterId로 단건 조회된다")
    void findByUserAndShelter_shouldReturnOne() {
        User user = user("user@gmail.com", "user");
        Shelter shelter = shelter(10L, "쉼터 1");

        Wish w = Wish.of(user, shelter);
        entityManager.persist(w);
        entityManager.flush();
        entityManager.clear();

        Optional<Wish> found = wishRepository.findByUserUserIdAndShelterShelterId(user.getUserId(), shelter.getShelterId());

        assertSoftly(softly -> {
            softly.assertThat(found).isPresent();
            softly.assertThat(found.get().getUser().getUserId()).isEqualTo(user.getUserId());
            softly.assertThat(found.get().getShelter().getShelterId()).isEqualTo(shelter.getShelterId());
        });
    }

    @Test
    @DisplayName("fetch join으로 쉼터를 함께 로딩하고 createdAt 내림차순으로 조회된다. ")
    void findAllWithShelterByUserId_shouldReturnLatestWithJoin() {
        User user = user("user@gmail.com", "user");
        Shelter shelter1 = shelter(11L, "쉼터 1");
        Shelter shelter2 = shelter(12L, "쉼터 2");

        // createdAt 정렬 확인을 위해 persist 전에 값 지정
        Wish wish1 = Wish.of(user, shelter1);
        ReflectionTestUtils.setField(wish1, "createdAt", LocalDateTime.now().minusMinutes(5));
        entityManager.persist(wish1);

        Wish wish2 = Wish.of(user, shelter2);
        ReflectionTestUtils.setField(wish2, "createdAt", LocalDateTime.now()); // 더 최신
        entityManager.persist(wish2);

        entityManager.flush();
        entityManager.clear();

        List<Wish> list = wishRepository.findAllWithShelterByUserIdOrderByCreatedAtDesc(user.getUserId());

        assertSoftly(softly -> {
            softly.assertThat(list).hasSize(2);
            // 내림차순: 최신 wish2 먼저
            softly.assertThat(list.get(0).getShelter().getShelterId()).isEqualTo(12L);
            softly.assertThat(list.get(1).getShelter().getShelterId()).isEqualTo(11L);
            // fetch join: 프록시 접근 가능(트랜잭션 내) + 즉시 로딩되어 N+1 방지
            softly.assertThat(list.get(0).getShelter().getName()).isEqualTo("쉼터 2");
            softly.assertThat(list.get(1).getShelter().getName()).isEqualTo("쉼터 1");
        });
    }

    private User user(String email, String nickname) {
        User user = new User(email, "pw123456!", nickname, "profile.jpg");
        entityManager.persist(user);
        return user;
    }

    private Shelter shelter(long id, String name) {
        Shelter shelter = Shelter.builder()
                .shelterId(id)
                .name(name)
                .address("서울")
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .build();
        entityManager.persist(shelter);
        return shelter;
    }
}
