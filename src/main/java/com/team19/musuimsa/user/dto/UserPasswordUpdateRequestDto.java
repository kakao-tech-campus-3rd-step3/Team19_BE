package com.team19.musuimsa.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordUpdateRequestDto(
        @NotBlank(message = "현재 비밀번호는 필수 입력값입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
        String newPassword
) {

}
