package com.team19.musuimsa.user.dto;

import com.team19.musuimsa.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 응답 데이터")
public record UserResponse(
        @Schema(description = "사용자 고유 ID", example = "1")
        Long userId,
        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,
        @Schema(description = "사용자 닉네임", example = "무더위쉼터탐험가")
        String nickname,
        @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
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
