package com.team19.musuimsa.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class S3UrlSigner {
    private final S3Presigner presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String signGetUrl(String objectKey, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(req);
        return presigned.url().toString();
    }
}

