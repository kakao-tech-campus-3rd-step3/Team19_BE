package com.team19.musuimsa.s3;

import com.team19.musuimsa.exception.external.S3UploadException;
import com.team19.musuimsa.exception.invalid.InvalidFileException;
import com.team19.musuimsa.exception.invalid.UnsupportedImageTypeException;
import com.team19.musuimsa.user.dto.UserPhotoUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPhotoUploader {

    private static final String USER_PREFIX = "users";
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.base-url}")
    private String s3PublicBaseUrl;

    public UserPhotoUpdateResponse upload(Long userId, MultipartFile multipartFile) {
        if (userId == null)
            throw new InvalidFileException("userId가 필요합니다.");
        if (multipartFile == null || multipartFile.isEmpty())
            throw new InvalidFileException("업로드할 파일이 비어있습니다.");

        long maxSize = 10 * 1024 * 1024; // 10MB
        if (multipartFile.getSize() > maxSize) {
            throw new InvalidFileException(
                    "파일 크기가 너무 큽니다. 최대 " + (maxSize / 1024 / 1024) + "MB까지 업로드 가능합니다.");
        }

        String contentType = multipartFile.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase())) {
            throw new UnsupportedImageTypeException(contentType);
        }

        String fileExtension = resolveFileExtension(multipartFile.getOriginalFilename(), contentType);
        String randomId = UUID.randomUUID().toString().replace("-", "");
        String objectKey = USER_PREFIX + "/" + userId + "/" + randomId + "." + fileExtension;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000")
                .build();

        try (InputStream inputStream = multipartFile.getInputStream()) {
            s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, multipartFile.getSize()));
        } catch (IOException e) {
            throw new S3UploadException(bucketName, objectKey, e);
        }

        String publicUrl = s3PublicBaseUrl.endsWith("/")
                ? s3PublicBaseUrl + objectKey
                : s3PublicBaseUrl + "/" + objectKey;

        return new UserPhotoUpdateResponse(objectKey, publicUrl, contentType, multipartFile.getSize());
    }

    private String resolveFileExtension(String originalFilename, String contentType) {
        if ("image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType)) {
            return "jpg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return "webp";
        }

        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        return "bin";
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        s3Client.deleteObject(b -> b.bucket(bucketName).key(objectKey));
    }
}
