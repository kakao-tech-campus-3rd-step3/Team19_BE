package com.team19.musuimsa.user.dto;

public record TokenResponseDto(
        String accessToken,
        String refreshToken
) {

}
