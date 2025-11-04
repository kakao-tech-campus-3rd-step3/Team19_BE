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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.team19.musuimsa.shelter.util.ShelterDtoUtils.average;
import static com.team19.musuimsa.shelter.util.ShelterDtoUtils.distanceFrom;
import static com.team19.musuimsa.shelter.util.ShelterDtoUtils.formatHours;

@Service
@RequiredArgsConstructor
@Transactional
public class WishService {

    private final WishRepository wishRepository;
    private final UserRepository userRepository;
    private final ShelterRepository shelterRepository;

    // 위시 추가
    public CreateWishResponse createWish(Long shelterId, User user) {
        Long authUserId = user.getUserId();

        Optional<Wish> existing = wishRepository.findByUserUserIdAndShelterShelterId(authUserId, shelterId);
        if (existing.isPresent()) {
            Wish wish = existing.get();
            return new CreateWishResponse(wish.getWishId(), authUserId, shelterId, wish.getCreatedAt());
        }

        User userRef = userRepository.getReferenceById(authUserId);

        Shelter shelterRef = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ShelterNotFoundException(shelterId));

        try {
            Wish savedWish = wishRepository.save(Wish.of(userRef, shelterRef));
            return new CreateWishResponse(savedWish.getWishId(), authUserId, shelterId, savedWish.getCreatedAt());
        } catch (DataIntegrityViolationException e) {
            Wish wish = wishRepository.findByUserUserIdAndShelterShelterId(authUserId, shelterId)
                    .orElseThrow(() -> e);
            return new CreateWishResponse(wish.getWishId(), authUserId, shelterId, wish.getCreatedAt());
        }
    }

    // 위시 조회
    @Transactional(readOnly = true)
    public WishListResponse getWishes(User user, Double latitude, Double longitude) {
        Long authUserId = user.getUserId();

        List<Wish> wishes = wishRepository.findAllWithShelterByUserIdOrderByCreatedAtDesc(authUserId);
        List<WishListItemResponse> items = new ArrayList<>();

        for (Wish wish : wishes) {
            Shelter shelter = wish.getShelter();

            String distance = null;
            if (shelter.getLatitude() != null && shelter.getLongitude() != null) {
                distance = distanceFrom(latitude, longitude, shelter);
            }

            String operatingHours = formatHours(
                    shelter.getWeekdayOpenTime(),
                    shelter.getWeekdayCloseTime()
            );

            Double averageRating = average(
                    shelter.getTotalRating(),
                    shelter.getReviewCount()
            );

            WishListItemResponse item = new WishListItemResponse(
                    shelter.getShelterId(),
                    shelter.getName(),
                    shelter.getAddress(),
                    operatingHours,
                    averageRating,
                    shelter.getPhotoUrl(),
                    distance
            );
            items.add(item);
        }

        return new WishListResponse(items);
    }

    // 위시 삭제
    public void deleteWish(Long shelterId, User user) {
        Long authUserId = user.getUserId();
        wishRepository.deleteByUserUserIdAndShelterShelterId(authUserId, shelterId);
    }
}
