package com.team19.musuimsa.wish.domain;

import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class WishTest {

    @Test
    @DisplayName("정적 팩토리로 생성하면 연관관계와 생성시각이 설정된다")
    void of_shouldCreateWithAssociationsAndTimestamp() {
        User user = Mockito.mock(User.class);
        Shelter shelter = Mockito.mock(Shelter.class);

        Wish wish = Wish.of(user, shelter);

        assertSoftly(softly -> {
            softly.assertThat(wish.getWishId()).as("미영속 PK").isNull();
            softly.assertThat(wish.getUser()).isSameAs(user);
            softly.assertThat(wish.getShelter()).isSameAs(shelter);
            softly.assertThat(wish.getCreatedAt())
                    .isNotNull()
                    .isBeforeOrEqualTo(LocalDateTime.now());
        });
    }
}
