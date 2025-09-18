package com.team19.musuimsa.exception.dto;

import org.springframework.http.HttpStatus;

public record ErrorResponseDto(
        int status,
        String error,
        String message,
        String path
) {

    public static ErrorResponseDto from(
            HttpStatus status,
            String message,
            String path) {

        return new ErrorResponseDto(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
    }
}
