package com.team19.musuimsa.user.dto;

import com.team19.musuimsa.user.domain.User;

public record UserResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
