package com.team19.musuimsa.user.dto;

import com.team19.musuimsa.user.domain.User;

public record UserResponseDto(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl
) {

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
