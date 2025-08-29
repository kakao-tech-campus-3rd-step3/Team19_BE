package com.team19.musuimsa.exception.dto;

public record ErrorResponseDto(
        int status,
        String error,
        String message,
        String path
) {

}
