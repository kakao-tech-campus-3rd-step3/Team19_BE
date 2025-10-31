package com.team19.musuimsa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "사용자 비밀번호 업데이트 요청 데이터")
public record UserPasswordUpdateRequest(
        @NotBlank(message = "현재 비밀번호는 필수 입력값입니다.")
        @Schema(description = "현재 비밀번호", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수 입력값입니다.")
        @Schema(description = "새 비밀번호", example = "password456@", requiredMode = Schema.RequiredMode.REQUIRED)
        String newPassword
) {

}
