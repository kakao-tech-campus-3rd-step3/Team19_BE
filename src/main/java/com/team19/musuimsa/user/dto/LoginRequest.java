package com.team19.musuimsa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 데이터")
public record LoginRequest(
        @NotBlank(message = "이메일은 필수 입력 값입니다.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @Schema(description = "사용자 이메일", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
        @Schema(description = "사용자 비밀번호", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        String password
) {

}
