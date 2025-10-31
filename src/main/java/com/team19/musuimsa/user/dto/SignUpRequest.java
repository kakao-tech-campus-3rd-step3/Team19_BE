package com.team19.musuimsa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청 데이터")
public record SignUpRequest(
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @Schema(description = "사용자 이메일 주소", example = "user@example.com", requiredMode = RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Schema(description = "사용자 비밀번호 (8~20자)", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String password,

        @NotBlank(message = "닉네임은 필수 입력 값입니다.")
        @Schema(description = "사용자 닉네임", example = "무더위쉼터탐험가", requiredMode = Schema.RequiredMode.REQUIRED)
        String nickname,

        @Schema(description = "사용자 프로필 이미지 URL (선택)", example = "https://example.com/profile.jpg")
        String profileImageUrl
) {

}
