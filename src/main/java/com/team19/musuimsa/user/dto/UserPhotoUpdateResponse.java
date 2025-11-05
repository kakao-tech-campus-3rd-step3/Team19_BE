package com.team19.musuimsa.user.dto;

public record UserPhotoUpdateResponse(
        String objectKey,
        String publicUrl,
        String contentType,
        long contentLength
) {
}