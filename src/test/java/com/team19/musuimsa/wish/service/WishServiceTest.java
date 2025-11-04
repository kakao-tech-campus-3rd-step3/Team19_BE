package com.team19.musuimsa.wish.service;

import com.team19.musuimsa.exception.notfound.ShelterNotFoundException;
import com.team19.musuimsa.shelter.domain.Shelter;
import com.team19.musuimsa.shelter.repository.ShelterRepository;
import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.user.repository.UserRepository;
import com.team19.musuimsa.wish.domain.Wish;
import com.team19.musuimsa.wish.dto.CreateWishResponse;
import com.team19.musuimsa.wish.dto.WishListItemResponse;
import com.team19.musuimsa.wish.dto.WishListResponse;
import com.team19.musuimsa.wish.repository.WishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    @Mock
    WishRepository wishRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    ShelterRepository shelterRepository;
    @InjectMocks
    WishService wishService;

    private long userId;
    private long shelterId;

    private User user;

    @BeforeEach
    void setUp() {
        userId = 1L;
        shelterId = 10L;

        user = principal(userId);
    }

    @Test
    @DisplayName("createWish - 이미 존재하면 저장 없이 기존 DTO를 반환한다. ")
    void createWish_shouldReturnExisting_whenAlreadyExists() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 10, 0);
        Shelter shelter = shelterBasic(shelterId, "무더위쉼터");
        Wish existing = wish(user, shelter, 100L, time);

        given(wishRepository.findByUserUserIdAndShelterShelterId(userId, shelterId))
                .willReturn(Optional.of(existing));

        CreateWishResponse dto = wishService.createWish(shelterId, user);

        then(wishRepository).should(never()).save(any(Wish.class));
        assertSoftly(softly -> {
            softly.assertThat(dto.wishId()).isEqualTo(100L);
            softly.assertThat(dto.userId()).isEqualTo(userId);
            softly.assertThat(dto.shelterId()).isEqualTo(shelterId);
            softly.assertThat(dto.createdAt()).isEqualTo(time);
        });
    }

    @Test
    @DisplayName("createWish - 존재하지 않으면 저장 후 DTO를 반환한다")
    void createWish_shouldPersist_whenNotExists() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 2, 9, 0);
        Shelter shelter = shelterBasic(shelterId, "무더위쉼터");

        given(wishRepository.findByUserUserIdAndShelterShelterId(userId, shelterId))
                .willReturn(Optional.empty());
        // 이 경로에서만 실제로 호출되므로 여기서 스텁
        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(shelterRepository.findById(shelterId)).willReturn(Optional.of(shelter));

        Wish saved = wish(user, shelter, 101L, time);
        given(wishRepository.save(any(Wish.class))).willReturn(saved);

        CreateWishResponse dto = wishService.createWish(shelterId, user);

        assertSoftly(softly -> {
            softly.assertThat(dto.wishId()).isEqualTo(101L);
            softly.assertThat(dto.userId()).isEqualTo(userId);
            softly.assertThat(dto.shelterId()).isEqualTo(shelterId);
            softly.assertThat(dto.createdAt()).isEqualTo(time);
        });
    }

    @Test
    @DisplayName("createWish - UNIQUE 경합 시 재조회로 멱등하게 복구한다")
    void createWish_shouldRecover_whenUniqueRace() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 3, 8, 0);
        Shelter shelter = shelterBasic(shelterId, "A");
        Wish after = wish(user, shelter, 102L, time);

        // 1차: 없음 -> 저장 시도 -> 중복 예외 -> 2차: 재조회 시 존재
        given(wishRepository.findByUserUserIdAndShelterShelterId(userId, shelterId))
                .willReturn(Optional.empty(), Optional.of(after));
        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(shelterRepository.findById(shelterId)).willReturn(Optional.of(shelter));
        given(wishRepository.save(any(Wish.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        CreateWishResponse dto = wishService.createWish(shelterId, user);

        assertSoftly(softly -> {
            softly.assertThat(dto.wishId()).isEqualTo(102L);
            softly.assertThat(dto.userId()).isEqualTo(userId);
            softly.assertThat(dto.shelterId()).isEqualTo(shelterId);
            softly.assertThat(dto.createdAt()).isEqualTo(time);
        });
    }

    @Test
    @DisplayName("createWish - 없는 쉼터면 ShelterNotFoundException을 던진다")
    void createWish_shouldThrow_whenShelterNotFound() {
        given(wishRepository.findByUserUserIdAndShelterShelterId(userId, shelterId))
                .willReturn(Optional.empty());
        given(userRepository.getReferenceById(userId)).willReturn(user);
        given(shelterRepository.findById(shelterId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> wishService.createWish(shelterId, user))
                .isInstanceOf(ShelterNotFoundException.class);
    }

    @Test
    @DisplayName("getWishes - 엔티티에서 DTO 목록으로 매핑한다(거리 null)")
    void getWishes_shouldMapEntitiesToDtos_whenNoCoords() {
        Shelter shelter = Shelter.builder()
                .shelterId(shelterId)
                .name("쉼터명")
                .address("서울")
                .latitude(null)
                .longitude(null)
                .weekdayOpenTime(LocalTime.of(9, 0))
                .weekdayCloseTime(LocalTime.of(18, 0))
                .totalRating(0).reviewCount(0)
                .build();

        Wish wish = Wish.of(user, shelter);
        given(wishRepository.findAllWithShelterByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(wish));

        WishListResponse list = wishService.getWishes(user, null, null);

        assertSoftly(softly -> {
            softly.assertThat(list.items()).hasSize(1);
            softly.assertThat(list.items().get(0).shelterId()).isEqualTo(shelterId);
            softly.assertThat(list.items().get(0).name()).isEqualTo("쉼터명");
            softly.assertThat(list.items().get(0).address()).isEqualTo("서울");
            softly.assertThat(list.items().get(0).operatingHours()).isEqualTo("09:00~18:00");
            softly.assertThat(list.items().get(0).averageRating()).isEqualTo(0.0);
            softly.assertThat(list.items().get(0).distance()).isNull();
        });
    }

    @Test
    @DisplayName("getWishes - 좌표가 있으면 거리(km, 소수 1자리)를 채워준다")
    void getWishes_shouldIncludeDistance_whenCoordsProvided() {
        Shelter shelter = Shelter.builder()
                .shelterId(shelterId).name("쉼터명").address("서울")
                .latitude(BigDecimal.valueOf(37.5651))
                .longitude(BigDecimal.valueOf(126.9895))
                .weekdayOpenTime(LocalTime.of(9, 0))
                .weekdayCloseTime(LocalTime.of(18, 0))
                .totalRating(0).reviewCount(0)
                .build();

        given(wishRepository.findAllWithShelterByUserIdOrderByCreatedAtDesc(userId))
                .willReturn(List.of(Wish.of(user, shelter)));

        WishListResponse list = wishService.getWishes(user, 37.5665, 126.9780);

        WishListItemResponse item = list.items().get(0);
        assertSoftly(softly -> {
            softly.assertThat(item.distance()).isNotNull();
            softly.assertThat(item.distance()).endsWith("km");
            softly.assertThat(item.distance()).startsWith("1.");
        });
    }


    private User principal(long id) {
        User user = Mockito.mock(User.class);
        Mockito.when(user.getUserId()).thenReturn(id);
        return user;
    }

    private Shelter shelterBasic(long id, String name) {
        return Shelter.builder()
                .shelterId(id)
                .name(name)
                .address("서울")
                .latitude(BigDecimal.valueOf(37.5665))
                .longitude(BigDecimal.valueOf(126.9780))
                .weekdayOpenTime(LocalTime.of(9, 0))
                .weekdayCloseTime(LocalTime.of(18, 0))
                .totalRating(0).reviewCount(0)
                .build();
    }

    private Wish wish(User user, Shelter shelter, long wishId, LocalDateTime createdAt) {
        Wish wish = Wish.of(user, shelter);
        ReflectionTestUtils.setField(wish, "wishId", wishId);
        ReflectionTestUtils.setField(wish, "createdAt", createdAt);
        return wish;
    }
}
