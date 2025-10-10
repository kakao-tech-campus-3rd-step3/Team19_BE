package com.team19.musuimsa.wish.controller;

import com.team19.musuimsa.user.domain.User;
import com.team19.musuimsa.wish.dto.CreateWishResponse;
import com.team19.musuimsa.wish.dto.WishListResponse;
import com.team19.musuimsa.wish.service.WishService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me/wishes")
public class WishController {

    private final WishService wishService;

    // 위시 추가
    @PostMapping("/{shelterId}")
    public ResponseEntity<CreateWishResponse> createWish(
            @PathVariable Long shelterId,
            @AuthenticationPrincipal User user
    ) {
        CreateWishResponse response = wishService.createWish(shelterId, user);

        URI location = URI.create("/api/users/me/wishes/" + response.wishId());
        return ResponseEntity.created(location).body(response);
    }

    // 위시 조회
    @GetMapping
    public ResponseEntity<WishListResponse> getWishes(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @AuthenticationPrincipal User user
    ) {
        WishListResponse response = wishService.getWishes(user, latitude, longitude);
        return ResponseEntity.ok(response);
    }

    // 위시 삭제
    @DeleteMapping("/{shelterId}")
    public ResponseEntity<Void> deleteWish(
            @PathVariable Long shelterId,
            @AuthenticationPrincipal User user
    ) {
        wishService.deleteWish(shelterId, user);
        return ResponseEntity.noContent().build();
    }
}
