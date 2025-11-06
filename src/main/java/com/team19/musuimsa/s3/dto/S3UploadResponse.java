package com.team19.musuimsa.s3.dto;

public record S3UploadResponse(
        String objectKey,
        String publicUrl,
        String contentType,
        long contentLength
) {
}