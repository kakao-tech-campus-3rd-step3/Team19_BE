package com.team19.musuimsa.user.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {

}
