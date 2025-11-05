package com.team19.musuimsa.exception.external;

public class S3UploadException extends ExternalApiException {
    public S3UploadException(String bucket, String objectKey, Throwable cause) {
        super("S3 업로드 실패: s3://" + bucket + "/" + objectKey, "s3://" + bucket + "/" + objectKey, cause);
    }
}
