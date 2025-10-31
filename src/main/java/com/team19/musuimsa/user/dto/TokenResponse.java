package com.team19.musuimsa.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 응답 데이터")
public record TokenResponse(
        @Schema(description = "Access Token", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0QGVtYWlsLmNvbSIsImV4cCI6MTc2MTc5MDcxMSwiaWF0IjoxNzYxNzg3MTExfQ.AJVtdTdhIZz-pysCUnc5Odo_JSZbjFbPLVy2Onn-MdiFXpbPNbto58JIwEDrlfsGBTCOMvndjzb-3quf50fbew")
        String accessToken,
        @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0QGVtYWlsLmNvbSIsImV4cCI6MTc2MjM5MTkxMSwiaWF0IjoxNzYxNzg3MTExfQ.ZdoERAVF1l11QIMoEoI6XQOBLXqfkHhqVsT6QWqOXOYNV4fSIAZ6KSOE8MxB1H5M8fkrZ_YG7OVR1ijbFJllkA")
        String refreshToken
) {

}
