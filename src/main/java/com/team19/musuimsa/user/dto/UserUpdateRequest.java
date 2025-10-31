package com.team19.musuimsa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 수정 요청 데이터")
public record UserUpdateRequest(
        @Schema(description = "사용자 닉네임", example = "무더위쉼터마스터")
        String nickname,
        @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/new-profile.jpg")
        String profileImageUrl
) {

}
