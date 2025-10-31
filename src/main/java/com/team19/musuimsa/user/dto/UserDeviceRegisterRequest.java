package com.team19.musuimsa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 디바이스 등록 요청 데이터")
public record UserDeviceRegisterRequest(
        @Schema(description = "디바이스 토큰", example = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz567890ab123cd4")
        String deviceToken
) {

}